package riscv

import chisel3.iotesters.{PeekPokeTester, Driver, ChiselFlatSpec}
import scala.util._
import chisel3.util._

class ALUTester extends ChiselFlatSpec {
    Driver(() => new ALU()) { c => new ALUUnitTester(c) }
}

class ALUUnitTester(c : ALU) extends PeekPokeTester(c) {
    def test(a : Int, b : Int, out : Long) = {
        poke(c.io.a, a)
        poke(c.io.b, b)
        expect(c.io.out, out)
    }

    poke(c.io.op, ALU.SLT)
    test(1, 2, 1)
    test(1, -1, 0)
    test(-2, -1, 1)

    poke(c.io.op, ALU.SLTU)
    test(1, 2, 1)
    test(1, -1, 1)
    test(-1, -2, 0)

    poke(c.io.op, ALU.SLL)
    test(1, 8, 256)
    poke(c.io.op, ALU.SRL)
    test(-1, 31, 1)
    poke(c.io.op, ALU.SRA)
    test(-4, 23, 0xffffffffL)
}