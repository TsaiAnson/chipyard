//See LICENSE for license details.

package firesim.firesim

import chisel3._
import chisel3.experimental.annotate

import freechips.rocketchip.config.{Field, Config, Parameters}
import freechips.rocketchip.diplomacy.{LazyModule}
import freechips.rocketchip.devices.debug.HasPeripheryDebugModuleImp
import freechips.rocketchip.subsystem.{CanHaveMasterAXI4MemPortModuleImp, CanHaveMasterAXI4MMIOPortModuleImp}
import freechips.rocketchip.tile.{RocketTile}
import sifive.blocks.devices.uart.HasPeripheryUARTModuleImp

import testchipip.{CanHavePeripherySerialModuleImp, CanHavePeripheryBlockDeviceModuleImp, CanHaveTraceIOModuleImp}
import icenet.CanHavePeripheryIceNICModuleImp

import junctions.{NastiKey, NastiParameters}
import midas.models.{FASEDBridge, AXI4EdgeSummary, CompleteConfig}
import midas.targetutils.{MemModelAnnotation}
import firesim.bridges._
import firesim.configs.MemModelKey
import tracegen.HasTraceGenTilesModuleImp
import ariane.ArianeTile

import memblade.cache.{HasDRAMCacheNoNICModuleImp, HasPeripheryDRAMCacheModuleImpValidOnly}
import memblade.client.HasPeripheryRemoteMemClientModuleImpValidOnly
import memblade.manager.HasPeripheryMemBladeModuleImpValidOnly

import boom.common.{BoomTile}

import chipyard.iobinders.{IOBinders, OverrideIOBinder, ComposeIOBinder}
import chipyard.HasChipyardTilesModuleImp

class WithSerialBridge extends OverrideIOBinder({
  (c, r, s, target: CanHavePeripherySerialModuleImp) =>
    target.serial.map(s => SerialBridge(target.clock, s)(target.p)).toSeq
})

class WithNICBridge extends OverrideIOBinder({
  (c, r, s, target: CanHavePeripheryIceNICModuleImp) =>
    target.net.map(n => NICBridge(target.clock, n)(target.p)).toSeq
})

class WithUARTBridge extends OverrideIOBinder({
  (c, r, s, target: HasPeripheryUARTModuleImp) =>
    target.uart.map(u => UARTBridge(target.clock, u)(target.p)).toSeq
})

class WithBlockDeviceBridge extends OverrideIOBinder({
  (c, r, s, target: CanHavePeripheryBlockDeviceModuleImp) =>
    target.bdev.map(b => BlockDevBridge(target.clock, b, target.reset.toBool)(target.p)).toSeq
})

class WithFASEDBridge extends OverrideIOBinder({
  (c, r, s, t: CanHaveMasterAXI4MemPortModuleImp) => {
    implicit val p = t.p
    (t.mem_axi4 zip t.outer.memAXI4Node).flatMap({ case (io, node) =>
      (io zip node.in).map({ case (axi4Bundle, (_, edge)) =>
        val nastiKey = NastiParameters(axi4Bundle.r.bits.data.getWidth,
                                       axi4Bundle.ar.bits.addr.getWidth,
                                       axi4Bundle.ar.bits.id.getWidth)
        val lastChannel = axi4Bundle == io.last
        FASEDBridge(t.clock, axi4Bundle, t.reset.toBool,
          CompleteConfig(
            p(firesim.configs.MemModelKey),
            nastiKey,
            Some(AXI4EdgeSummary(edge)),
            lastChannel))
      })
    }).toSeq
  }
})

class WithFASEDMMIOBridge extends OverrideIOBinder({
  (c, r, s, t: CanHaveMasterAXI4MMIOPortModuleImp) => {
    implicit val p = t.p
    (t.mmio_axi4 zip t.outer.mmioAXI4Node.in).map { case (io, (_, edge)) =>
      val nastiKey = NastiParameters(io.r.bits.data.getWidth,
                                     io.ar.bits.addr.getWidth,
                                     io.ar.bits.id.getWidth)
      FASEDBridge(t.clock, io, t.reset.toBool,
        CompleteConfig(
          p(firesim.configs.MemModelKey),
          nastiKey,
          Some(AXI4EdgeSummary(edge)),
          true))
    }.toSeq
  }
})

class WithTracerVBridge extends OverrideIOBinder({
  (c, r, s, target: CanHaveTraceIOModuleImp) => target.traceIO match {
    case Some(t) => t.traces.map(tileTrace => TracerVBridge(tileTrace)(target.p))
    case None    => Nil
  }
})


class WithTraceGenBridge extends OverrideIOBinder({
  (c, r, s, target: HasTraceGenTilesModuleImp) =>
    Seq(GroundTestBridge(target.clock, target.success)(target.p))
})

class WithFireSimMultiCycleRegfile extends ComposeIOBinder({
  (c, r, s, target: HasChipyardTilesModuleImp) => {
    target.outer.tiles.map {
      case r: RocketTile => {
        annotate(MemModelAnnotation(r.module.core.rocketImpl.rf.rf))
        r.module.fpuOpt.foreach(fpu => annotate(MemModelAnnotation(fpu.fpuImpl.regfile)))
      }
      case b: BoomTile => {
        val core = b.module.core
        core.iregfile match {
          case irf: boom.exu.RegisterFileSynthesizable => annotate(MemModelAnnotation(irf.regfile))
          case _ => Nil
        }
        if (core.fp_pipeline != null) core.fp_pipeline.fregfile match {
          case frf: boom.exu.RegisterFileSynthesizable => annotate(MemModelAnnotation(frf.regfile))
          case _ => Nil
        }
      }
      case a: ArianeTile => Nil
    }
    Nil
  }
})

class WithDRAMCacheBridge extends OverrideIOBinder({
  (c, r, s, t: HasPeripheryDRAMCacheModuleImpValidOnly) => {
    implicit val p = t.p
    val io = t.cache_axi4
    val node = t.outer.outAXI4Node
    val axiBridges = (io zip node.in).map({ case (axi4Bundle, (_, edge)) =>
      val nastiKey = NastiParameters(axi4Bundle.r.bits.data.getWidth,
                                     axi4Bundle.ar.bits.addr.getWidth,
                                     axi4Bundle.ar.bits.id.getWidth)
      val lastChannel = axi4Bundle == io.last
      FASEDBridge(t.clock, axi4Bundle, t.reset.toBool,
        CompleteConfig(
          p(firesim.configs.MemModelKey),
          nastiKey,
          Some(AXI4EdgeSummary(edge)),
          lastChannel))
    }).toSeq
    val nicBridge = NICBridge(t.clock, t.net)
    nicBridge +: axiBridges
  }
})

class WithMemBladeBridge extends OverrideIOBinder({
  (c, r, s, t: HasPeripheryMemBladeModuleImpValidOnly) =>
    Seq(NICBridge(t.clock, t.net)(t.p))
})

class WithRemoteMemClientBridge extends OverrideIOBinder({
  (c, r, s, t: HasPeripheryRemoteMemClientModuleImpValidOnly) =>
    Seq(NICBridge(t.clock, t.net)(t.p))
})

// Shorthand to register all of the provided bridges above
class WithDefaultFireSimBridges extends Config(
  new chipyard.iobinders.WithGPIOTiedOff ++
  new chipyard.iobinders.WithTiedOffDebug ++
  new chipyard.iobinders.WithTieOffInterrupts ++
  new WithSerialBridge ++
  new WithNICBridge ++
  new WithUARTBridge ++
  new WithBlockDeviceBridge ++
  new WithFASEDBridge ++
  new WithFASEDMMIOBridge ++
  new WithFireSimMultiCycleRegfile ++
  new WithTracerVBridge
)
