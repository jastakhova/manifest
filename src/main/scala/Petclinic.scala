import main.scala._
import scala.collection.immutable._

object Petclinic {

  def main(args: Array[String]) {
    Application.build({ implicit context =>

      val appFork = Input("Source control fork", "dieu")
      val appBranch = Input("Source control branch", "HEAD")

      val identity = EnvParam("identity").value
      val credential = EnvParam("credential").value

      val db = Cluster("db", identity, credential)
      val app = Cluster("app", identity, credential, 2)
      val lb = Cluster("lb", identity, credential)

      val cookbookPath = "http://cookbooks"

      db.recipe("mysql::server", cookbookPath,
        Map("mysql" ->
          Map(
            "server_root_password" -> "…",
            "server_repl_password" -> "…",
            "server_debian_password" -> "..."))
      ).recipe("war::mysql", cookbookPath,
        Map("database" ->
          Map(
            "name" -> "petclinic",
            "schema" -> "…",
            "data" -> "..."))
      )

      val lbWithHaproxy = lb.recipe("haproxy", cookbookPath)

      var appWithTomcat = app.recipe("tomcat", cookbookPath)

      lbWithHaproxy -> appWithTomcat

      val appConfigured = appWithTomcat.recipe("war", "http://",
        Map("war" -> Map("deploy" -> Map("git" -> Map(
          "url" -> "git",
          "revision" -> appBranch.value
        ))))
      ).recipe("war::configure", "http://",
        Map("configure" -> Map(
          "source" -> "https://",
          "to" -> "https://",
          "variables" ->
            Map("hosts" -> db.result.value.ips.mkString(","),
              "database" -> "petclinic")
        ))
      )

      val haproxyRebalanced = lbWithHaproxy.recipe("war::lb", "http://cookbooks",
        Map("haproxy.rebalance" -> Map("nodes" -> app.result.value.ips.mkString(",")))
      )

      appConfigured -> haproxyRebalanced

      Output("db-hosts", "Database IP address", db.result.value.ips.mkString(","))
      Output("app-hosts", "Application IP address", app.result.value.ips.mkString(","))
      Output("lb-hosts", "Load Balancer IP address", lb.result.value.ips.mkString(","))
      Output("app-url", "Url to the application", "http://" + lb.result.value.ips.head)
      Output("haproxy-url", "Url to haproxy stats", "http://" + lb.result.value.ips.head + ":22002/")
    })
  }
}

