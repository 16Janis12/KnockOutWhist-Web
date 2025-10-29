package util

import scala.util.Random

object GameUtil {

  private val CharPool: String = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
  private val CodeLength: Int = 6
  private val MaxRepetition: Int = 2
  private val random = new Random()

  def generateCode(): String = {
    val freq = Array.fill(CharPool.length)(0)
    val code = new StringBuilder(CodeLength)

    for (_ <- 0 until CodeLength) {
      var index = random.nextInt(CharPool.length)
      // Pick a new character if it's already used twice
      while (freq(index) >= MaxRepetition) {
        index = random.nextInt(CharPool.length)
      }
      freq(index) += 1
      code.append(CharPool.charAt(index))
    }

    code.toString()
  }
  
}
