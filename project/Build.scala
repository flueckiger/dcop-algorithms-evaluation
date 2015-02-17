import scala.xml.Attribute
import scala.xml.Elem
import scala.xml.Node
import scala.xml.Null
import scala.xml.transform.RewriteRule

import com.typesafe.sbteclipse.core.Validation
import com.typesafe.sbteclipse.plugin.EclipsePlugin.EclipseTransformerFactory

import sbt.ProjectRef
import sbt.State
import scalaz.Success

object ResourcesRule extends RewriteRule {
  override def transform(n: Node): Seq[Node] =
    n match {
      case e: Elem if e.label == "classpathentry" && (e \ "@kind").text == "src" && (e \ "@path").text.endsWith("resources") =>
        e % Attribute(null, "excluding", "**/*.java|**/*.scala", Null)
      case e => e
    }
}

object ResourcesTransformer extends EclipseTransformerFactory[RewriteRule] {
  override def createTransformer(ref: ProjectRef, state: State): Validation[RewriteRule] =
    Success(ResourcesRule)
}
