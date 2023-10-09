package qouteall.imm_ptl.core.portal.animation;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.q_misc_util.Helper;
import qouteall.q_misc_util.my_util.Vec2d;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class NormalAnimation implements PortalAnimationDriver {
    public static void init() {
        PortalAnimationDriver.registerDeserializer(
            new ResourceLocation("imm_ptl:normal"),
            NormalAnimation::deserialize
        );
    }
    
    public static record Phase(
        long durationTicks,
        DeltaUnilateralPortalState delta,
        TimingFunction timingFunction
    ) {
        public static Phase fromTag(CompoundTag tag) {
            long durationTicks = tag.getLong("durationTicks");
            DeltaUnilateralPortalState delta = DeltaUnilateralPortalState.fromTag(tag.getCompound("delta"));
            TimingFunction timingFunction = TimingFunction.fromString(tag.getString("timingFunction"));
            return new Phase(durationTicks, delta, timingFunction);
        }
        
        public CompoundTag toTag() {
            CompoundTag tag = new CompoundTag();
            tag.putLong("durationTicks", durationTicks);
            tag.put("delta", delta.toTag());
            tag.putString("timingFunction", timingFunction.name());
            return tag;
        }
        
        public Phase getFlippedVersion() {
            return new Phase(durationTicks, delta.getFlipped(), timingFunction);
        }
    
        public Component getInfo() {
            return Component.literal("Phase(%d,".formatted(durationTicks))
                .append(delta.toString())
                .append(")");
        }
    
        public static class Builder {
            private long durationTicks = 0;
            private DeltaUnilateralPortalState delta = DeltaUnilateralPortalState.identity;
            private TimingFunction timingFunction = TimingFunction.linear;
            
            public Builder durationTicks(long durationTicks) {
                this.durationTicks = durationTicks;
                return this;
            }
            
            public Builder delta(DeltaUnilateralPortalState delta) {
                this.delta = delta;
                return this;
            }
            
            public Builder timingFunction(TimingFunction timingFunction) {
                this.timingFunction = timingFunction;
                return this;
            }
            
            public Phase build() {
                return new Phase(durationTicks, delta, timingFunction);
            }
        }
    }
    
    public final List<Phase> phases;
    public final long startingGameTime;
    public final int loopCount;
    
    private final long ticksPerRound;
    
    public NormalAnimation(
        List<Phase> phases,
        long startingGameTime,
        int loopCount
    ) {
        this.phases = phases;
        this.startingGameTime = startingGameTime;
        this.loopCount = loopCount;
        
        long totalTicks = 0;
        for (Phase phase : phases) {
            totalTicks += phase.durationTicks;
        }
        ticksPerRound = totalTicks;
    }
    
    private static NormalAnimation deserialize(CompoundTag compoundTag) {
        UnilateralPortalState initialState = UnilateralPortalState.fromTag(compoundTag.getCompound("initialState"));
        
        List<Phase> phases = Helper.listTagToList(
            Helper.getCompoundList(compoundTag, "phases"),
            Phase::fromTag
        );
        
        long startingGameTime = compoundTag.getLong("startingGameTime");
        
        int loopCount = compoundTag.getInt("loopCount");
        
        if (phases.isEmpty() || loopCount < 0) {
            throw new RuntimeException("invalid NormalAnimation");
        }
        
        return new NormalAnimation(
            phases,
            startingGameTime,
            loopCount
        );
    }
    
    @Override
    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        
        tag.putString("type", "imm_ptl:normal");
        tag.put("phases", Helper.listToListTag(phases, Phase::toTag));
        tag.putLong("startingGameTime", startingGameTime);
        tag.putInt("loopCount", loopCount);
        
        return tag;
    }
    
    private long getTotalDuration() {
        if (loopCount >= 100000) {
            return Long.MAX_VALUE;
        }
        
        return ticksPerRound * loopCount;
    }
    
    @Override
    public AnimationResult getAnimationResult(long tickTime, float partialTicks, AnimationContext context) {
        if (ticksPerRound == 0 || phases.isEmpty()) {
            Helper.err("Invalid NormalAnimation");
            return new AnimationResult(null, true);
        }
        
        double passedTicks = ((double) (tickTime - 1 - startingGameTime)) + partialTicks;
        long totalDuration = getTotalDuration();
        
        boolean ends = false;
        if (passedTicks >= totalDuration - 1) {
            passedTicks = totalDuration - 1;
            ends = true;
        }
        
        if (passedTicks < -1) {
            if (context.isClientSide()) {
                return new AnimationResult(null, false);
            }
            else {
                Helper.err("NormalAnimation starts in the future");
                return new AnimationResult(null, true);
            }
        }
        
        // modulo works for double!
        double passedTicksInThisRound = passedTicks % ((double) ticksPerRound);
        long roundIndex = Math.floorDiv((long) passedTicks, ticksPerRound);
        
        long traversedTicks = 0;
        DeltaUnilateralPortalState lastDelta = DeltaUnilateralPortalState.identity;
        for (Phase phase : phases) {
            if (phase.durationTicks != 0 && passedTicksInThisRound < traversedTicks + phase.durationTicks) {
                double phaseProgress = (1 + passedTicksInThisRound - traversedTicks) / (double) phase.durationTicks;
                phaseProgress = phase.timingFunction.mapProgress(phaseProgress);
                
                DeltaUnilateralPortalState interpolated = DeltaUnilateralPortalState.interpolate(
                    lastDelta, phase.delta, phaseProgress
                );
                return new AnimationResult(
                    interpolated,
                    ends
                );
            }
            else {
                lastDelta = phase.delta();
                traversedTicks += phase.durationTicks;
            }
        }
        
        return new AnimationResult(lastDelta, ends);
    }
    
    @Nullable
    @Override
    public DeltaUnilateralPortalState getEndingResult(long tickTime, AnimationContext context) {
        if (phases.isEmpty()) {
            return null;
        }
        return phases.get(phases.size() - 1).delta();
    }
    
    @Override
    public PortalAnimationDriver getFlippedVersion() {
        return new NormalAnimation(
            phases.stream().map(phase -> phase.getFlippedVersion()).collect(Collectors.toList()),
            startingGameTime,
            loopCount
        );
    }
    
    // generated by GitHub Copilot
    public static class Builder {
        private List<Phase> phases = new ArrayList<>();
        private long startingGameTime;
        private int loopCount;
        
        public Builder() {
        }
        
        public Builder phases(List<Phase> phases) {
            this.phases = phases;
            return this;
        }
        
        public Builder startingGameTime(long startingGameTime) {
            this.startingGameTime = startingGameTime;
            return this;
        }
        
        public Builder loopCount(int loopCount) {
            this.loopCount = loopCount;
            return this;
        }
        
        public NormalAnimation build() {
            return new NormalAnimation(phases, startingGameTime, loopCount);
        }
    }
    
    public static NormalAnimation createSizeAnimation(
        Portal portal, Vec2d startSizeScale, Vec2d toSizeScale,
        long startingGameTime, long durationTicks,
        TimingFunction timingFunction
    ) {
        Phase initialPhase = new Phase.Builder()
            .durationTicks(0)
            .timingFunction(timingFunction)
            .delta(new DeltaUnilateralPortalState.Builder()
                .scaleSize(startSizeScale)
                .build()
            )
            .build();
        
        Phase endingPhase = new Phase.Builder()
            .durationTicks(durationTicks)
            .timingFunction(timingFunction)
            .delta(new DeltaUnilateralPortalState.Builder()
                .scaleSize(toSizeScale)
                .build()
            )
            .build();
        
        return new Builder()
            .phases(List.of(initialPhase, endingPhase))
            .startingGameTime(startingGameTime)
            .loopCount(1)
            .build();
    }
    
    @Override
    public Component getInfo() {
        MutableComponent component = Component.literal("Normal[\n");
        for (Phase phase : phases) {
            component.append(" ");
            component.append(phase.getInfo());
            component.append("\n");
        }
        component.append("] %d times".formatted(loopCount));
        return component;
    }
}
