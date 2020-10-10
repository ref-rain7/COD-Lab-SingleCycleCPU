package riscv

import chisel3.stage.ChiselStage
import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}

object main {
    def main(args: Array[String]) {
        //println((new ChiselStage).emitVerilog(new Register))

        Driver(() => new RegisterFile()) { c => new RegisterTester(c) }
    }
}