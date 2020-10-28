package riscv

import chisel3.iotesters.{PeekPokeTester, Driver, ChiselFlatSpec}
import scala.util._
import chisel3.util._

import scala.io.Source

class SingleCycleTester extends ChiselFlatSpec {
    Driver(() => new SingleCycleCPU) { c => new SingleCycleUnitTester(c) }
}

class SingleCycleUnitTester(c : SingleCycleCPU) extends PeekPokeTester(c) {
    // entry of testNodes(i) is at addr 4 * (i+1), where i >= 1 and i <= testNodes.length
    val testNodes = Array(
        "add", "addi", "sub", "slt", "slti", "sltu", "sltiu",
        "and", "andi", "lui", "or", "ori", "xor", "xori",
        "sll", "slli", "srl", "srli", "sra", "srai", "auipc",
        "beq", "bne", "bge", "begu", "blt", "bltu", "jal", "jalr"
    )
    val passInstrAddr = 0x2b28
    val failInstrAddr = 0x2b24

    def initInstrMem() : Array[Long] = {
        val src = Source.fromFile("src/test/scala/riscv/ram.txt")
        val mem = new Array[Long](0x2000)
        for (line <- src.getLines()) {
            val s = line.trim.stripPrefix("@").split(' ')
            if (!s(0).isEmpty) {
                val addr = BigInt(s(0), 16).toInt
                mem(addr) = BigInt(s(1), 16).toLong
            }
        }
        src.close()
        return mem
    }

    def test(): Unit = {
        val mem = initInstrMem()
        for (i <- 0 until 0x2800) {
            val addr = peek(c.io.instr_addr).toLong
            //println(addr.toHexString)
            poke(c.io.instr_data, mem(addr/4))

            if (addr > 0 && addr <= testNodes.length * 4) {
                println("testing " + addr.toHexString + ": " + testNodes(addr/4-1))
            } else if (addr == passInstrAddr) {
                println("ALL TEST PASSED!!!")
                return
            } else if (addr == failInstrAddr) {
                println("FAILED...")
                return
            }
            step(1)
        }
    }

    test()
}