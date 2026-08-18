package de.saschat.cmos.items;

import de.saschat.cmos.ExposureComputerMod;
import de.saschat.cmos.blocks.tiles.WirelessReceiverTile;
import de.saschat.cmos.registry.ComponentRegistry;
import de.saschat.cmos.util.Location;
import de.saschat.cmos.*;
import io.github.mortuusars.exposure.data.ColorPalettes;
import io.github.mortuusars.exposure.server.CameraInstances;
import io.github.mortuusars.exposure.world.camera.CameraId;
import io.github.mortuusars.exposure.world.camera.ExposureType;
import io.github.mortuusars.exposure.world.camera.capture.CaptureParameters;
import io.github.mortuusars.exposure.world.camera.capture.DitherMode;
import io.github.mortuusars.exposure.world.camera.film.properties.FilmProperties;
import io.github.mortuusars.exposure.world.camera.film.properties.FilmStyle;
import io.github.mortuusars.exposure.world.camera.frame.Frame;
import io.github.mortuusars.exposure.world.entity.CameraHolder;
import io.github.mortuusars.exposure.world.item.camera.Attachment;
import io.github.mortuusars.exposure.world.item.camera.CameraItem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.*;
import java.util.List;
import java.util.Optional;

public class CmosCameraItem extends CameraItem {

    public FilmProperties filmProperties;

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (hand == InteractionHand.MAIN_HAND
                && player.getOffhandItem().getItem() instanceof CameraItem offhandCameraItem
                && offhandCameraItem.isActive(player.getOffhandItem())) {
            return InteractionResultHolder.pass(stack);
        }

        if (!isActive(stack)) {
            return activateInHand(player, stack, hand);
        }

        return release((CameraHolder) (Object) player, stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> components, TooltipFlag tooltipFlag) {
        Location q = stack.get(ComponentRegistry.LOCATION.get());

        if(q != null) {
            BlockPos p = q.pos();
            components.add(Component.translatable("message.cmosexposure.linked_to", p.getX(), p.getY(), p.getZ()));
        } else {
            components.add(Component.translatable("message.cmosexposure.unlinked"));
        }

        super.appendHoverText(stack, context, components, tooltipFlag);
    }

    @Override
    public InteractionResult useOn(UseOnContext useOnContext) {
        BlockEntity blockEntity = useOnContext.getLevel().getBlockEntity(useOnContext.getClickedPos());
        if(blockEntity instanceof WirelessReceiverTile) {
            useOnContext.getItemInHand().set(ComponentRegistry.LOCATION.get(), new Location(
                    useOnContext.getClickedPos(),
                    useOnContext.getLevel().dimension()
            ));
            if(useOnContext.getPlayer() instanceof ServerPlayer)
                useOnContext.getPlayer().sendSystemMessage(Component.translatable("message.cmosexposure.linked"));
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.FAIL;
    }

    private static FilmProperties fp = new FilmProperties(ExposureType.COLOR, Optional.of(128), ColorPalettes.DEFAULT, DitherMode.CLEAN, FilmStyle.EMPTY);
    public CmosCameraItem(Properties properties) {
        super(properties);
        this.filmProperties = fp;
    }

    @Override
    public @NotNull FilmProperties getFilmProperties(ItemStack stack) {
        return this.filmProperties;
    }

    protected @NotNull List<Attachment<?>> defineAttachments() {
        return List.of(Attachment.FLASH, Attachment.LENS, Attachment.FILTER);
    }
    
    @Override
    public boolean hasAttachmentsMenu() {
        return true;
    }

    private static ResourceLocation cc = ResourceLocation.fromNamespaceAndPath(ExposureComputerMod.MOD_ID, "cmos_camera");
    @Override
    public ResourceLocation getCaptureType(ItemStack stack) {
        return cc;
    }

    @Override
    public float getScaleOnStand() {
        return 1f;
    }

    @Override
    public boolean canTakePhoto(CameraHolder holder, ItemStack stack) {
        return  isLinked(stack)
                && !isOnCooldown(holder, stack)
                && !getTimer().isTicking(holder, stack)
                && !getShutter().isOpen(stack)
                && CameraInstances.canReleaseShutter(CameraId.ofStack(stack));
    }

    private boolean isLinked(ItemStack stack) {
        Location q = stack.get(ComponentRegistry.LOCATION.get());
        return q != null;
    }

    @Override
    public int calculateCooldownAfterShot(ItemStack stack, CaptureParameters captureParameters) {
        Double cd = Config.Server.CMOS_CAMERA_COOLDOWN.get();
        if (Config.Server.CMOS_CAMERA_DO_COOLDOWN_MODIFIERS.get()) {
            Integer res_cooldown = Config.Server.COOLDOWN_RESOLUTION.get();
            Double bw_cooldown = Config.Server.COOLDOWN_BW.get();
            Double silent_cooldown = Config.Server.COOLDOWN_SILENT.get();
            boolean isBnW;
            Integer size;
            try {
                Field f = CaptureParameters.class.getDeclaredField("filmProperties");
                f.setAccessible(true);
                FilmProperties fp = (FilmProperties)f.get(captureParameters);
                size = fp.getSize();
                Field f2 = FilmProperties.class.getDeclaredField("type");
                f2.setAccessible(true);
                ExposureType et = (ExposureType)f2.get(fp);
                isBnW = et.getSerializedName() == "black_and_white";
            } catch(Exception e) {
                size = filmProperties.getSize();
                isBnW = false; // cannot be determined without extracting from fp so take the safer route
            }
            Double res_double = res_cooldown.doubleValue();
            Double size_double = size.doubleValue();
            cd *= (size_double*size_double)/(res_double*res_double);
            if (isBnW) {
                cd *= bw_cooldown;
            }
            if (stack.getEntityRepresentation().isSilent()) {
                cd *= silent_cooldown;
            }
        }
        return cd.intValue();
    }

    @Override
    public void addFrameToFilm(ItemStack stack, Frame frame) {

        Location q = stack.get(ComponentRegistry.LOCATION.get());
        if(q != null) {
            ServerLevel level = ExposureComputerMod.SERVER.getLevel(q.level());
            BlockEntity blockEntity = level.getBlockEntity(q.pos());

            if(blockEntity instanceof WirelessReceiverTile tile) {
                tile.receiveFrame(frame);
            }
        }
    }

}
