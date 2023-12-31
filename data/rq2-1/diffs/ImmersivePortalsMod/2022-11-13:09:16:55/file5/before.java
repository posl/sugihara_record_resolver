package qouteall.imm_ptl.core.portal.animation;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import qouteall.q_misc_util.Helper;

@Deprecated
public class PositionAnimation implements PortalAnimationDriver {
    public static void init() {
        PortalAnimationDriver.registerDeserializer(
            new ResourceLocation("imm_ptl:position"),
            PositionAnimation::deserialize
        );
    }
    
    public final Vec3 from;
    public final Vec3 to;
    public final long startGameTime;
    public final long endGameTime;
    
    public PositionAnimation(Vec3 from, Vec3 to, long startGameTime, long endGameTime) {
        this.from = from;
        this.to = to;
        this.startGameTime = startGameTime;
        this.endGameTime = endGameTime;
    }
    
    private static PositionAnimation deserialize(CompoundTag tag) {
        Vec3 from = Helper.getVec3d(tag, "from");
        Vec3 to = Helper.getVec3d(tag, "to");
        long startGameTime = tag.getLong("startGameTime");
        long endGameTime = tag.getLong("endGameTime");
        
        return new PositionAnimation(from, to, startGameTime, endGameTime);
    }
    
    @Override
    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        
        tag.putString("type", "imm_ptl:position");
        Helper.putVec3d(tag, "from", from);
        Helper.putVec3d(tag, "to", to);
        tag.putLong("startGameTime", startGameTime);
        tag.putLong("endGameTime", endGameTime);
        
        return tag;
    }
    
    @Override
    public boolean update(UnilateralPortalState.Builder stateBuilder, long tickTime, float partialTicks) {
        
        double progress = (tickTime - startGameTime + partialTicks) / (double) (endGameTime - startGameTime);
        
        if (progress >= 1) {
            stateBuilder.position(to);
            return true;
        }
        else {
            stateBuilder.position(Helper.interpolatePos(from, to, progress));
            return false;
        }
    }
    
    // generated by GitHub copilot
    public static class Builder {
        public Vec3 from;
        public Vec3 to;
        public long startGameTime;
        public long endGameTime;
        
        public PositionAnimation build() {
            return new PositionAnimation(from, to, startGameTime, endGameTime);
        }
        
        public Builder setFrom(Vec3 from) {
            this.from = from;
            return this;
        }
        
        public Builder setTo(Vec3 to) {
            this.to = to;
            return this;
        }
        
        public Builder setStartGameTime(long startGameTime) {
            this.startGameTime = startGameTime;
            return this;
        }
        
        public Builder setEndGameTime(long endGameTime) {
            this.endGameTime = endGameTime;
            return this;
        }
    }
}
