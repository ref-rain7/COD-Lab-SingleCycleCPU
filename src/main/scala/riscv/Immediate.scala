package riscv

import chisel3._
import chisel3.util._

import Controller._

class Immediate extends Module {
    val io = IO(new Bundle {
        val sel = Input(UInt(3.W))
        val instr = Input(UInt(32.W))
        val out = Output(UInt(32.W))
    })

    io.out := MuxLookup(io.sel, Cat(Fill(20, io.instr(31)), io.instr(31, 20)),
        Seq(
            IMM_I -> Cat(Fill(20, io.instr(31)), io.instr(31, 20)),
            IMM_S -> Cat(Fill(20, io.instr(31)), io.instr(31, 25), io.instr(11, 7)),
            IMM_B -> Cat(Fill(20, io.instr(31)), io.instr(7), io.instr(30, 25), io.instr(11, 8), 0.B),
            IMM_U -> Cat(io.instr(31, 12), Fill(12, 0.B)),
            IMM_J -> Cat(Fill(12, io.instr(31)), io.instr(19, 12), io.instr(20), io.instr(30, 21), 0.B)
        )
    )
}