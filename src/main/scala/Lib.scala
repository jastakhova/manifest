package main.scala

/**
 * User: Julia Astakhova
 * Date: 5/7/13
 */
// Input parameters that are configured before launching manifest
case class Input(description: String, defaultValue: String)(implicit context: Context) {
  def value: String = ""
}
// Output parameters that will be available on the instance page after manifest was successfully executed
case class Output(name: String, description: String, value: String)(implicit context: Context)
// Environment Parameters that are specified in environment or cloud provider
case class EnvParam(name: String)(implicit context: Context) {
  def value: String = ""
}

// Promise for some value in the future. It is used to return values from the phases
class Promise[T] {
  def map[N](f: T => N): Promise[N] = new Promise
  def value: T = Nil.asInstanceOf[T]
}

// Phase of the execution that can return some result
case class PhaseWithResult[R](name: String)(steps: Context => Unit)(implicit context: Context){
  def result: Promise[R] = new Promise[R]()
}

// Output of the provision stage
class ProvisionVmsOutput{
  def ips: List[String] = List()
}

case class Cluster(clusterName1: String, identity1: String, credential1: String, quantity1: Int = 1)(implicit context: Context)
  extends ClusterPhase[ProvisionVmsOutput](clusterName1, identity1, credential1, quantity1)("provisionVms")(context)

// Node or some number of the nodes configured similarly that are provisioned
class ClusterPhase[R](clusterName: String, identity: String, credential: String, quantity: Int = 1)(phaseName: String)(implicit context: Context)
  extends PhaseWithResult[R](phaseName)({_ =>})(context) {

  def recipe(recipeName: String, cookbooksURL: String, params: Map[String, Any] = Map())(implicit context: Context): ClusterPhase[R] = this
}

// Context is used to bind phase nesting and order (inside phases and the command)
trait Context {}

// Some part of the deployment
case class Phase(name: String)(steps: Context => Unit)(implicit context: Context) {

  // Parts of the deployment can be bound in terms of dependecy.
  // This method allows to point which phases the current one should wait for
  def waits(phases: Phase*): Phase = this

  // returns the subsequent phase
  def ->(subsequent: Phase): Phase = this
}

case class Workflow(name: String)(steps: Context => Unit)

case class DestroyWorkflow(steps: Context => Unit) extends Workflow("destroy")(steps)

// Manifest application in total
object Application {

  // Builds and outputs to STDOUT string yml manifest
  // Has workflows as parameters. Ensures that there is at least launch workflow
  def build(launch: Context => Unit, workflows: Workflow*) { println("<builded manifest>") }
}

