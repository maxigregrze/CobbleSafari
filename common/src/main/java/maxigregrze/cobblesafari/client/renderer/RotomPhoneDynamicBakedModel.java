package maxigregrze.cobblesafari.client.renderer;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Function;

public class RotomPhoneDynamicBakedModel implements BakedModel {

    private final BakedModel fallback;
    private final Function<ResourceLocation, BakedModel> lookup;
    private final ItemOverrides overrides;

    public RotomPhoneDynamicBakedModel(BakedModel fallback, Function<ResourceLocation, BakedModel> lookup) {
        this.fallback = fallback;
        this.lookup = lookup;
        this.overrides = new ItemOverrides() {
            @Override
            public BakedModel resolve(BakedModel model, ItemStack stack, @Nullable net.minecraft.client.multiplayer.ClientLevel level, @Nullable LivingEntity entity, int seed) {
                ResourceLocation variant = RotomPhoneTextureResolver.modelForStack(stack);
                BakedModel resolved = RotomPhoneDynamicBakedModel.this.lookup.apply(variant);
                if (resolved == null || resolved == net.minecraft.client.Minecraft.getInstance().getModelManager().getMissingModel()) {
                    return RotomPhoneDynamicBakedModel.this.fallback;
                }
                return resolved;
            }
        };
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable net.minecraft.world.level.block.state.BlockState state, @Nullable Direction direction, RandomSource random) {
        return fallback.getQuads(state, direction, random);
    }

    @Override
    public boolean useAmbientOcclusion() { return fallback.useAmbientOcclusion(); }

    @Override
    public boolean isGui3d() { return fallback.isGui3d(); }

    @Override
    public boolean usesBlockLight() { return fallback.usesBlockLight(); }

    @Override
    public boolean isCustomRenderer() { return fallback.isCustomRenderer(); }

    @Override
    public TextureAtlasSprite getParticleIcon() { return fallback.getParticleIcon(); }

    @Override
    public ItemTransforms getTransforms() { return fallback.getTransforms(); }

    @Override
    public ItemOverrides getOverrides() { return overrides; }
}
