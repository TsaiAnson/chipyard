package firesim.firesim

import freechips.rocketchip.config.{Parameters, Config}
import freechips.rocketchip.subsystem._

import firesim.bridges._
import firesim.configs._


//**********************************************************************************
//* EE290-2 Project FireSim Gemmini Configurations
//*********************************************************************************/

class FireSimGemminiBfloatRocketConfig extends Config(
  new WithInclusiveCache ++
  new gemmini.BfloatGemminiConfig ++
  new WithNBigCores(1) ++
  new FireSimRocketConfig)


class FireSimGemminiFloatRocketConfig extends Config(
  new WithInclusiveCache ++
  new gemmini.FloatGemminiConfig ++
  new WithNBigCores(1) ++
  new FireSimRocketConfig)
