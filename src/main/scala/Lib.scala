package main.scala

/**
 * User: Julia Astakhova
 * Date: 5/7/13
 */
// Input parameters that are configured before launching manifest
case class Input(description: String, defaultValue: String)(implicit application: Application) {
  def value: String = ""
}
// Output parameters that will be available on the instance page after manifest was successfully executed
case class Output(name: String, description: String, value: String)(implicit application: Application)
// Environment Parameters that are specified in environment or cloud provider
case class EnvParam(name: String)(implicit application: Application) {
  def value: String = ""
}

// Promise for some value in the future. It is used to return values from the phases
class Promise[T] {
  def map[N](f: T => N): Promise[N] = new Promise
  def value: T = Nil.asInstanceOf[T]
}

// Phase of the execution that can return some result
case class PhaseWithResult[R](override val name: String)(steps: Context => Unit)(implicit context: Context)
  extends Phase(name: String)(steps: Context => Unit) {
  def result: Promise[R] = new Promise[R]()
}

// Output of the provision stage
class ProvisionVmsOutput{
  def ips: List[String] = List()
}

// Node or some number of the nodes configured similarly
case class Cluster(name: String, quantity: Int = 1)(implicit application: Application) {

  // Execution of the recipe that acts as a phase
  case class Recipe(recipeName: String)(steps: Context => Unit)(implicit context: Context) extends Phase(name: String)(steps: Context => Unit) {
    def :<(params: Map[String, Any]): Recipe = this
  }

  // Node can be asked to be provisioned. Configuration for provisioning is provided as parameters
  def provisionVms(identity: String, credential: String)(implicit context: Context): PhaseWithResult[ProvisionVmsOutput] = {
    val doNothing: Context => Unit  = {_ =>}
    new PhaseWithResult[ProvisionVmsOutput]("provisionVms")(doNothing)(context)
  }

  // Node can be asked to run chef recipe on it
  def recipe(name: String, cookbooksURL: String)(implicit context: Context): Recipe = {
    val doNothing: Context => Unit  = {_ =>}
    new Recipe(name)(doNothing)(context)
  }
}

// Context is used to bind phase nesting and order (inside phases and the command)
trait Context {}

// Some part of the deployment
case class Phase(name: String)(steps: Context => Unit)(implicit context: Context) {

  // Parts of the deployment can be bound in terms of dependecy.
  // This method allows to point which phases the current one should wait for
  def waits(phases: Phase*): Phase = this
}

// Upper-level command that will be available to be run on the instance from the ui intance page
case class Command(name: String)(steps: Context => Unit)(implicit application: Application)

// Manifest application in total
class Application {

  // Build and output to STDOUT string yml manifest
  def build { println("<builded manifest>") }
}

