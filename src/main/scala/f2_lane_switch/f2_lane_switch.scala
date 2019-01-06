// See LICENSE for license details.
package f2_lane_switch
import chisel3._
import chisel3.util._
import chisel3.experimental._
import dsptools._
import dsptools.numbers._

class f2_lane_switch_io [ T <: Data ] (
     proto     : T,
     todspios  : Int=4,
     fromdspios: Int=1,
     serdesios : Int=6
) extends Bundle {
    //input comes from Lane and goes to SerDes
    val from_dsp             = Vec(fromdspios,Flipped(DecoupledIO(proto.cloneType)))
    val to_dsp               = Vec(todspios,DecoupledIO(proto.cloneType))
    val to_dsp_mode          = Vec(todspios,Input(UInt(2.W))) //Off/on/scan
    val dsp_to_serdes_address= Vec(serdesios,Input(UInt(log2Ceil(fromdspios).W))) //every serdes has 7 sources
    val serdes_to_dsp_address= Vec(todspios,Input(UInt(log2Ceil(serdesios).W)))  //7 dsp dspios has 6 serdes sources
    val from_serdes          = Vec(serdesios,Flipped(DecoupledIO(proto.cloneType)))
    val to_serdes            = Vec(serdesios,DecoupledIO(proto.cloneType))
    val to_serdes_mode       = Vec(serdesios,Input(UInt(2.W))) //Off/On/memory
    //These are scans to feed constant to serdes 
    val from_serdes_scan     = Vec(serdesios,Flipped(DecoupledIO(proto.cloneType)))
    val from_dsp_scan        = Vec(serdesios,Flipped(DecoupledIO(proto.cloneType)))
    override def cloneType = (new f2_lane_switch_io(
        proto=proto.cloneType, 
        todspios=todspios,
        fromdspios=fromdspios, 
        serdesios=serdesios )).asInstanceOf[this.type]

}

class f2_lane_switch [ T <: Data ] (
        proto     : T,
        todspios  : Int=4,
        fromdspios: Int=1,
        serdesios : Int=6 
    ) extends Module {

    val io = IO( 
        new f2_lane_switch_io(
            proto=proto.cloneType,
            fromdspios=fromdspios,
            todspios=todspios,
            serdesios=serdesios
        )
    )
    val iozero = 0.U.asTypeOf(proto.cloneType)
    
    // Every input must have a decoupler to all connected outputs
    //from_dsp_decouplers=Vec(fromdspios, Module(new decouple_branch(proto=proto.cloneType,serdesios)).io)
    //from_serdes_scan_decouplers=Vec(serdesios, Module(new decouple_branch(proto=proto.cloneType,serdesios)).io)
    //from_dsp_scan_decouplers=Vec(serdesios, Module(new decouple_branch(proto=proto.cloneType,serdesios)).io)
    //for ( i <- 0 to fromdspios-1 ) {
    //    for ( k <- 0 to serdesios-1 ) {
    //    from_dsp_decouplers(i).Ai<>io.
    //}  

    //Defaults
    io.from_serdes_scan.map(_.ready:=false.B)
    io.from_dsp_scan.map(_.ready:=false.B)
    io.from_dsp.map(_.ready:=false.B)
    io.from_serdes.map(_.ready:=false.B)
    io.to_dsp.map(_.valid:=false.B)
    io.to_serdes.map(_.valid:=false.B)
    //From SerDes to dsp routing

    //Addressable inputs are always ready
    io.from_serdes.map(_.ready:=true.B)
    io.from_serdes_scan.map(_.ready:=true.B)
    io.from_dsp.map(_.ready:=true.B)


    for ( i <- 0 to todspios-1 ) {
        when ( io.to_dsp_mode(i)===0.U) {
            io.to_dsp(i).valid:=1.U
            io.to_dsp(i).bits:=iozero
        } .elsewhen ( io.to_dsp_mode(i)===1.U) {
            io.to_dsp(i).valid:=io.from_serdes(io.serdes_to_dsp_address(i)).valid
            when ( io.to_dsp(i).ready) {
                io.to_dsp(i).bits:=io.from_serdes(io.serdes_to_dsp_address(i)).bits
            }.otherwise {
                io.to_dsp(i).bits:=iozero
            }
        } .elsewhen ( io.to_dsp_mode(i)===2.U) {
            io.to_dsp(i).valid:=io.from_serdes_scan(io.serdes_to_dsp_address(i)).valid
            when ( io.to_dsp(i).ready) {
                io.to_dsp(i).bits:=io.from_serdes_scan(io.serdes_to_dsp_address(i)).bits
            }.otherwise {
                io.to_dsp(i).bits:=iozero
            }
        } .otherwise {
            io.to_dsp(i).valid:=1.U
            io.to_dsp(i).bits:=iozero
        }

    }

    //From Dsp to SerDes routing
    for ( i <- 0 to serdesios-1) {
        when ( io.to_serdes_mode(i)===0.U) {
            io.to_serdes(i).valid:=1.U
            io.to_serdes(i).bits:=iozero
        } .elsewhen ( io.to_serdes_mode(i)===1.U) {
            io.to_serdes(i).valid:=io.from_dsp(io.dsp_to_serdes_address(i)).valid
            when ( io.to_serdes(i).ready) {
                io.to_serdes(i).bits:=io.from_dsp(io.dsp_to_serdes_address(i)).bits
            }.otherwise {
                io.to_serdes(i).bits:= iozero
            }
        } .elsewhen ( io.to_serdes_mode(i)===2.U) {
            io.to_serdes(i).valid:=io.from_dsp_scan(io.dsp_to_serdes_address(i)).valid
            when ( io.to_serdes(i).ready) {
                io.to_serdes(i).bits:=io.from_dsp_scan(io.dsp_to_serdes_address(i)).bits
            }.otherwise {
                io.to_serdes(i).bits:= iozero
            }
        } .otherwise { 
            io.to_serdes(i).valid:=1.U
            io.to_serdes(i).bits:=iozero
       }
   }
}

//This gives you verilog
object f2_lane_switch extends App {
  chisel3.Driver.execute(args, () => new f2_lane_switch(proto=UInt(8.W), todspios=4, fromdspios=1, serdesios=6))
}

