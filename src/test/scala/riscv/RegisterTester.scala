package riscv

import chisel3.iotesters.{PeekPokeTester, Driver, ChiselFlatSpec}
import scala.util._
import chisel3.util._

class RegisterTester(c: RegisterFile) extends PeekPokeTester(c) {
    def readExpect1(addr: Int, value: Int): Unit = {
        poke(c.io.raddr1, addr)
        expect(c.io.rdata1, value)
    }
    def readExpect2(addr: Int, value: Int): Unit = {
        poke(c.io.raddr2, addr)
        expect(c.io.rdata2, value)
    }
    def write(addr: Int, value: Int): Unit = {
        poke(c.io.wen, 1)
        poke(c.io.wdata, value)
        poke(c.io.waddr, addr)
        step(1)
        poke(c.io.wen, 0)
    }

    val randomSeq = Seq.fill(32){Random.nextInt(Int.MaxValue)}
    for (i <- 0 until 32) {
        write(addr = i, value = randomSeq(i))
    }

    for (i <- 0 until 1000) {
        val rand1 = Random.nextInt(32)
        val rand2 = Random.nextInt(32)
        readExpect1(addr = rand1, value = if (rand1 == 0) 0 else randomSeq(rand1))
        readExpect2(addr = rand2, value = if (rand2 == 0) 0 else randomSeq(rand2))
    }
}
