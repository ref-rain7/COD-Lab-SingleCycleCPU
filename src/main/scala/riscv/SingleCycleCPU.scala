package riscv

import chisel3._
import chisel3.util._
import scala.collection._

import Controller._

class Comparator extends Module {
    val io = IO(new Bundle {
        val a = Input(UInt(32.W))
        val b = Input(UInt(32.W))
        val unsigned = Input(Bool())
        val equal = Output(Bool())
        val lessThan = Output(Bool())
    })
    io.equal := ~((io.a ^ io.b).orR())
    io.lessThan := Mux(io.unsigned, io.a < io.b, io.a.asSInt < io.b.asSInt)
}

class SingleCycleCPU extends Module {
    val io = IO(new Bundle {
        val instr_addr = Output(UInt(32.W))
        val instr_data = Input(UInt(32.W))
//        val mem_addr = Output(UInt(32.W))
//        val mem_rdata = Input(UInt(32.W))
//        val mem_wen = Output(Bool())
//        val mem_wdata = Output(UInt(32.W))
    })
    val pc = RegInit(0.U(32.W))
    val pc_plus4 = Wire(UInt(32.W))

    val reg = Module(new RegisterFile)
    val alu = Module(new ALU)
    val imm = Module(new Immediate)
    val cmp = Module(new Comparator)
    val ctrl = Module(new Controller)

    ctrl.io.instrType := Cat(io.instr_data(30), io.instr_data(14, 12), io.instr_data(6, 2))
    ctrl.io.branchCmpEq := cmp.io.equal
    ctrl.io.branchCmpLT := cmp.io.lessThan
    cmp.io.a := reg.io.rdata1
    cmp.io.b := reg.io.rdata2
    cmp.io.unsigned := ctrl.io.branchCmpUnsigned

    io.instr_addr := pc
    pc_plus4 := pc + 4.U
    pc := MuxLookup(ctrl.io.pcSel, pc_plus4, Seq (
        PC_SEL_PLUS4 -> pc_plus4,
        PC_SEL_ALU -> Cat(alu.io.out(31, 1), 0.B) // set LSB to '0'
    ))

    reg.io.raddr1 := io.instr_data(19, 15)
    reg.io.raddr2 := io.instr_data(24, 20)
    reg.io.waddr := io.instr_data(11, 7)
    reg.io.wen := ctrl.io.regWen
    reg.io.wdata := MuxLookup(ctrl.io.regWdataSel, alu.io.out, Seq(
        REG_WRDATA_SEL_ALU -> alu.io.out,
        REG_WRDATA_SEL_PC_PLUS4 -> pc_plus4
    ))

    imm.io.instr := io.instr_data
    imm.io.sel := ctrl.io.immSel

    alu.io.op := ctrl.io.aluCtrl
    alu.io.a := MuxLookup(ctrl.io.aluASel, reg.io.rdata1, Seq (
        A_SEL_REG -> reg.io.rdata1,
        A_SEL_PC -> pc
    ))
    alu.io.b := MuxLookup(ctrl.io.aluBSel, reg.io.rdata2, Seq(
        B_SEL_REG -> reg.io.rdata2,
        B_SEL_IMM -> imm.io.out
    ))

//    printf("\timm: %d, sel: %d\n", imm.io.out, ctrl.io.immSel)
//    printf("\tcmp: a:%d, b:%d\n", reg.io.rdata1, reg.io.rdata2)
}