package riscv

import chisel3._
import chisel3.util._
import scala.collection._

object ALU {
    val ADD = 0.U(4.W)
    val SUB = 1.U(4.W)

    val SLT = 2.U(4.W)
    val SLTU = 3.U(4.W)

    val SLL = 4.U(4.W)
    val SRL = 5.U(4.W)
    val SRA = 6.U(4.W)

    val XOR = 7.U(4.W)
    val OR = 8.U(4.W)
    val AND = 9.U(4.W)
}

import ALU._

class ALU extends Module {
    val io = IO(new Bundle {
        val a = Input(UInt(32.W))
        val b = Input(UInt(32.W))
        val op = Input(UInt(4.W))
        val out = Output(UInt(32.W))
    })

    var shifter = Module(new BarrelShifter)
    shifter.io.in := io.a
    shifter.io.shamt := io.b(4, 0)

    shifter.io.op := MuxLookup (io.op, 0.U(4.W),
        Seq (
            SLL -> BarrelShifter.SLL,
            SRL -> BarrelShifter.SRL,
            SRA -> BarrelShifter.SRA
        )
    )

    io.out := MuxLookup (io.op, io.a,
        Seq (
            ADD -> (io.a + io.b),
            SUB -> (io.a - io.b),
            OR -> (io.a | io.b),
            AND -> (io.a & io.b),
            XOR -> (io.a ^ io.b),

            SLT -> Cat(0.U(31.W), (io.a.asSInt < io.b.asSInt)),
            SLTU -> Cat(0.U(31.W), (io.a < io.b)),

            SLL -> shifter.io.out,
            SRL -> shifter.io.out,
            SRA -> shifter.io.out
        )
    )
}