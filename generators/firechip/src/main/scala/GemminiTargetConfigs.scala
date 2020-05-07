package firesim.firesim

import freechips.rocketchip.config.{Parameters, Config}
import freechips.rocketchip.subsystem._

import firesim.bridges._
import firesim.configs._


//**********************************************************************************
//* EE290-2 Project FireSim Gemmini Configurations
//*********************************************************************************/

class FireSimBfloatGemminiRocketConfig extends Config(
  new WithInclusiveCache ++
  new gemmini.GemminiBfloatConfig ++
  new WithNBigCores(1) ++
  new FireSimRocketConfig)


class FireSimFloatGemminiRocketConfig extends Config(
  new WithInclusiveCache ++
  new gemmini.GemminiFloatConfig ++
  new WithNBigCores(1) ++
  new FireSimRocketConfig)
