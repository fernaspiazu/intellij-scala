object test {
  class AAA(i: Int)
}

object test2 {
  import test.{AAA => BBB}

  val x = new BBB(<caret>)
}
//TEXT: i: Int, STRIKEOUT: false