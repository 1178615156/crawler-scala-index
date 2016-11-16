import scala.reflect.ClassTag

val SC = implicitly[ClassTag[String]]
"sss" match {
  case SC(ss) => ss
}