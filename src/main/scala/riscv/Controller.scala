package riscv

import chisel3._
import chisel3.util._
import scala.collection._


object Controller {
    val PC_SEL_PLUS4 = 0.U(1.W)
    val PC_SEL_ALU = 1.U(1.W)

    val IMM_I = 0.U(3.W)
    val IMM_S = 2.U(3.W)
    val IMM_B = 3.U(3.W)
    val IMM_U = 4.U(3.W)
    val IMM_J = 5.U(3.W)

    val A_SEL_REG = 0.U(1.W)
    val A_SEL_PC = 1.U(1.W)

    val B_SEL_REG = 0.U(1.W)
    val B_SEL_IMM = 1.U(1.W)

    val REG_WRDATA_SEL_ALU = 0.U(1.W)
    val REG_WRDATA_SEL_PC_PLUS4 = 1.U(1.W)
}

import Instruction._
import  Controller._

class Controller extends Module {
    val io = IO(new Bundle {
        val instrType = Input(UInt(9.W)) /* instr[30], instr[14:12], instr[6:2] */

        val branchCmpEq = Input(Bool())
        val branchCmpLT = Input(Bool())
        val branchCmpUnsigned = Output(Bool())

        val regWen = Output(Bool())
        val pcSel = Output(UInt(1.W))
        val immSel = Output(UInt(3.W))
        val aluASel = Output(UInt(1.W))
        val aluBSel = Output(UInt(1.W))
        val aluCtrl = Output(UInt(4.W))
        val regWdataSel = Output(UInt(1.W))
    })

    /* defaults */
    io.regWen := false.B
    io.pcSel := PC_SEL_PLUS4
    io.immSel := IMM_I
    io.aluASel := A_SEL_REG
    io.aluBSel := B_SEL_REG
    io.aluCtrl := "hf".U // meaningless
    io.regWdataSel := REG_WRDATA_SEL_ALU

/* Branch Instructions:
 * funct3[2]: '0': select EQUAL signal from comparator as branch condition
 *            '1': select LESS_THAN signal from comparator as branch condition
 * funct3[1]: '0': comparison is signed,
 *            '1': comparison is unsigned
 * funct3[0]: '0': take branch if signal selected by funct[2] is '1'
 *            '1': take branch if signal selected by funct[2] is '0'
 */
    private val funct3 = Wire(UInt(3.W))
    private val branchCond = Wire(Bool())
    private val takeBranch = Wire(Bool())
    private val pcSelWhenBranch = Wire(UInt(1.W))
    funct3 := io.instrType(7, 5)
    io.branchCmpUnsigned := funct3(1)
    branchCond := Mux(funct3(2), io.branchCmpLT, io.branchCmpEq)
    takeBranch := Mux(funct3(0), !branchCond, branchCond)
    pcSelWhenBranch := Mux(takeBranch, PC_SEL_ALU, PC_SEL_PLUS4)


    when (io.instrType === LUI) {
        io.immSel := IMM_U
        io.aluBSel := B_SEL_IMM; io.aluCtrl := ALU.COPY_B
        io.regWen := true.B
    }
    .elsewhen (io.instrType === AUIPC) {
        io.immSel := IMM_U
        io.aluASel := A_SEL_PC; io.aluBSel := B_SEL_IMM; io.aluCtrl := ALU.ADD
        io.regWen := true.B
    }
    .elsewhen (io.instrType === JAL) {
        io.pcSel := PC_SEL_ALU
        io.immSel := IMM_J
        io.aluASel := A_SEL_PC; io.aluBSel := B_SEL_IMM; io.aluCtrl := ALU.ADD
        io.regWen := true.B; io.regWdataSel := REG_WRDATA_SEL_PC_PLUS4
    }
    .elsewhen (io.instrType === JALR) {
        io.pcSel := PC_SEL_ALU
        io.aluASel := A_SEL_REG; io.aluBSel := B_SEL_IMM; io.aluCtrl := ALU.ADD
        io.regWen := true.B; io.regWdataSel := REG_WRDATA_SEL_PC_PLUS4
    }


    private val branchInstrs = Array(BEQ, BNE, BLT, BGE, BLTU, BGEU)
    for (t <- branchInstrs) {
        when (io.instrType === t) {
            io.pcSel := pcSelWhenBranch
            io.immSel := IMM_B
            io.aluASel := A_SEL_PC; io.aluBSel := B_SEL_IMM; io.aluCtrl := ALU.ADD
        }
    }

    private val regImmInstrArgs = Array(
        (ADDI, ALU.ADD), (SLTI, ALU.SLT), (SLTIU, ALU.SLTU),
        (XORI, ALU.XOR), (ORI, ALU.OR), (ANDI, ALU.AND),
        (SLLI, ALU.SLL), (SRLI, ALU.SRL), (SRAI, ALU.SRA)
    )
    for (t <- regImmInstrArgs) {
        when (io.instrType === t._1) {
            io.regWen := true.B
            io.aluBSel := B_SEL_IMM; io.aluCtrl := t._2
        }
    }

    private val regRegInstrArgs = Array(
        (ADD, ALU.ADD), (SUB, ALU.SUB), (SLL, ALU.SLL), (SLT, ALU.SLT), (SLTU, ALU.SLTU),
        (XOR, ALU.XOR), (SRL, ALU.SRL), (SRA, ALU.SRA), (OR, ALU.OR), (AND, ALU.AND)
    )
    for (t <- regRegInstrArgs) {
        when (io.instrType === t._1) {
            io.regWen := true.B
            io.aluCtrl := t._2
        }
    }

}
