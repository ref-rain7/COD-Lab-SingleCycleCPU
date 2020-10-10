package riscv

import chisel3.iotesters.{PeekPokeTester, Driver, ChiselFlatSpec}
import scala.util._
import chisel3.util._

class ShifterTester extends ChiselFlatSpec {
    Driver(() => new BarrelShifter()) { c => new ShifterUnitTester(c) }
}

class ShifterUnitTester(c : BarrelShifter) extends PeekPokeTester(c) {
    def expectedOutput(op: Int, in: Int, shamt: Int): Long = {
        val t: Long = op match {
            case 0 => in << shamt
            case 1 => in >>> shamt
            case 2 => in >> shamt
            case 3 => in << (32 - shamt) | in >>> shamt
        }
        return t & 0xffffffffL
    }

    for (i <- 0 until 4) {
        poke(c.io.op, i)
        for (j <- 0 until 1000) {
            val amt = Random.nextInt(32)
            val data = Random.nextInt()
            poke(c.io.in, data)
            poke(c.io.shamt, amt)
            expect(c.io.out, expectedOutput(i, data, amt))
        }
    }
}