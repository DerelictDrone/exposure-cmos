package de.saschat.cmos.blocks.peripheral;

import dan200.computercraft.api.lua.*;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import de.saschat.cmos.ExposureComputerMod;
import de.saschat.cmos.blocks.tiles.StandControllerTile;
import de.saschat.cmos.mixin.duck.CameraStandEntityDuck;
import de.saschat.cmos.items.*;
import io.github.mortuusars.exposure.world.camera.*;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.*;

import io.github.mortuusars.exposure.data.*;
import io.github.mortuusars.exposure.util.color.Color;
import io.github.mortuusars.exposure.server.CameraInstances;
import io.github.mortuusars.exposure.world.camera.CameraId;
import io.github.mortuusars.exposure.world.camera.ExposureType;
import io.github.mortuusars.exposure.world.camera.capture.CaptureParameters;
import io.github.mortuusars.exposure.world.camera.capture.DitherMode;
import io.github.mortuusars.exposure.world.camera.film.properties.*;
import io.github.mortuusars.exposure.world.camera.film.properties.FilmProperties;
import io.github.mortuusars.exposure.world.camera.film.properties.FilmStyle;
import io.github.mortuusars.exposure.world.camera.frame.Frame;
import io.github.mortuusars.exposure.world.entity.CameraHolder;
import io.github.mortuusars.exposure.world.item.camera.Attachment;
import io.github.mortuusars.exposure.world.item.camera.CameraItem;
import io.github.mortuusars.exposure.world.level.LevelUtil;
import io.github.mortuusars.exposure.world.level.storage.ExposureIdentifier;



import io.github.mortuusars.exposure.world.camera.component.*;
// import io.github.mortuusars.exposure.world.camera.component.FlashMode;
// import io.github.mortuusars.exposure.world.camera.component.FocalRange;
// import io.github.mortuusars.exposure.world.camera.component.ShutterSpeed;
import io.github.mortuusars.exposure.world.entity.CameraOperator;
import io.github.mortuusars.exposure.world.entity.CameraStandEntity;
import io.github.mortuusars.exposure.world.item.camera.CameraItem;
import io.github.mortuusars.exposure.world.item.camera.CameraSettings;
import io.github.mortuusars.exposure.world.item.camera.ShutterState;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;

import java.util.*;

import java.lang.reflect.*;

// todo 1. javadoc all, 2. check fabric, 3. create "event attach ability" events for stand controller on found/lost stand controller, 3a. do this over tick%20 events, 4. fix rotation values
public class StandControllerPeripheral implements IPeripheral {
    StandControllerTile tile;

    public StandControllerPeripheral(BlockEntity ent, Direction dir) {
        this.tile = (StandControllerTile) ent;
        tile.peripherals.add(this);
    }

    public List<IComputerAccess> computers = new LinkedList<>();

    @Override
    public void attach(IComputerAccess computer) {
        IPeripheral.super.attach(computer);
        computers.add(computer);
    }

    @Override
    public void detach(IComputerAccess computer) {
        IPeripheral.super.detach(computer);
        computers.remove(computer);
    }

    private void closeCamera() {
        CameraOperator operator;
        if (tile.getStandEntity().getCamera().getItem() instanceof CameraItem x) {
            if ((operator = tile.getStandEntity().operator()) != null) {
                operator.removeActiveExposureCamera();
                x.deactivate(operator.asOperatorEntity(), tile.getStandEntity().getCamera());
            }
        }
    }


    @LuaFunction
    public final boolean isPresent() {
        return tile.getStandEntity() != null;
    }

    @LuaFunction
    public final boolean isMalfunctioned() {
        CameraStandEntity standEntity = tile.getStandEntity();
        if (standEntity != null) {
            return standEntity.isMalfunctioned();
        } else return false;
    }

    @LuaFunction
    public final double getYaw() {
        CameraStandEntity standEntity = tile.getStandEntity();
        if (standEntity != null) {
            return standEntity.getYRot();
        } else return -1;
    }

    @LuaFunction
    public final double getPitch() {
        CameraStandEntity standEntity = tile.getStandEntity();
        if (standEntity != null) {
            return standEntity.getXRot();
        } else return -1;
    }

    @LuaFunction
    public final boolean setYaw(double yaw) {
        CameraStandEntity standEntity = tile.getStandEntity();
        if (standEntity != null) {
            closeCamera();
            standEntity.setYRot((float) yaw % 360);
            standEntity.syncRotationToClients();
            return true;
        } else return false;
    }

    @LuaFunction
    public final boolean setPitch(double pitch) {
        CameraStandEntity standEntity = tile.getStandEntity();
        if (standEntity != null) {
            closeCamera();
            standEntity.setXRot((float) pitch % 360);
            standEntity.syncRotationToClients();
            return true;
        } else return false;
    }


    @LuaFunction
    public final boolean trigger() {
        CameraStandEntity standEntity = tile.getStandEntity();
        if (standEntity != null && !standEntity.getCamera().isEmpty() && standEntity.getCamera().getItem() instanceof CameraItem item) {
            closeCamera();
            if (item.canTakePhoto(standEntity, standEntity.getCamera())) {
                ExposureComputerMod.SERVER.submit(standEntity::release);
                return true;
            }
        }
        return false;
    }

    @LuaFunction
    public final boolean canTrigger() {
        CameraStandEntity standEntity = tile.getStandEntity();
        if (standEntity != null && !standEntity.getCamera().isEmpty() && standEntity.getCamera().getItem() instanceof CameraItem item) {
            closeCamera();
            if (item.canTakePhoto(standEntity, standEntity.getCamera())) {
                return true;
            }
        }
        return false;
    }

    @LuaFunction
    public final Map<Integer, String> getAvailableShutterSpeeds() {
        CameraStandEntity standEntity = tile.getStandEntity();
        if (standEntity != null && !standEntity.getCamera().isEmpty() && standEntity.getCamera().getItem() instanceof CameraItem item) {
            Map<Integer, String> speeds = new HashMap<>();
            int i = 1;
            for (ShutterSpeed availableShutterSpeed : item.getAvailableShutterSpeeds()) {
                speeds.put(i, availableShutterSpeed.getNotation());
                i++;
            }
            return speeds;
        }
        return null;
    }

    @LuaFunction
    public final String getShutterSpeed() {
        CameraStandEntity standEntity = tile.getStandEntity();
        if (standEntity != null && !standEntity.getCamera().isEmpty() && standEntity.getCamera().getItem() instanceof CameraItem item) {
            ShutterState state = item.getShutter().getState(standEntity.getCamera());
            return state.shutterSpeed().getNotation();
        }
        return null;
    }

    @LuaFunction
    public final boolean setShutterSpeed(String shutterSpeed) {
        CameraStandEntity standEntity = tile.getStandEntity();
        if (standEntity != null && !standEntity.getCamera().isEmpty() && standEntity.getCamera().getItem() instanceof CameraItem item) {
            ShutterSpeed select = null;
            for (ShutterSpeed availableShutterSpeed : item.getAvailableShutterSpeeds()) {
                if (availableShutterSpeed.getNotation().equals(shutterSpeed)) {
                    select = availableShutterSpeed;
                }
            }
            if (select != null) {
                closeCamera();
                CameraSettings.SHUTTER_SPEED.set(standEntity.getCamera(), select);
                return true;
            }
        }
        return false;
    }

    @LuaFunction
    public final Map<Integer, Integer> getFocalRange() {
        ServerLevel level = (ServerLevel) tile.getLevel();
        CameraStandEntity standEntity = tile.getStandEntity();
        if (standEntity != null && !standEntity.getCamera().isEmpty() && standEntity.getCamera().getItem() instanceof CameraItem item) {
            FocalRange focalRange = item.getFocalRange(level.registryAccess(), standEntity.getCamera());
            return Map.of(1, focalRange.min(), 2, focalRange.max());
        }
        return null;
    }
    @LuaFunction
    public final double getZoom() {
        CameraStandEntity standEntity = tile.getStandEntity();
        if (standEntity != null && !standEntity.getCamera().isEmpty() && standEntity.getCamera().getItem() instanceof CameraItem item) {
            return CameraSettings.ZOOM.get(standEntity.getCamera());
        }
        return -1;
    }
    @LuaFunction
    public final boolean setZoom(double zoom) {
        CameraStandEntity standEntity = tile.getStandEntity();
        if (standEntity != null && !standEntity.getCamera().isEmpty() && standEntity.getCamera().getItem() instanceof CameraItem item) {
            closeCamera();
            CameraSettings.ZOOM.set(standEntity.getCamera(), Math.min(Math.max((float) zoom, 0), 1));
            return true;
        }
        return false;
    }

    @LuaFunction
    public final boolean setFlash(int flash) {
        CameraStandEntity standEntity = tile.getStandEntity();
        if (standEntity != null && !standEntity.getCamera().isEmpty() && standEntity.getCamera().getItem() instanceof CameraItem item) {
            closeCamera();
            int idx = ((int) Math.abs(flash)) % FlashMode.values().length;
            CameraSettings.FLASH_MODE.set(standEntity.getCamera(), FlashMode.values()[idx]);
            return true;
        }
        return false;
    }
    @LuaFunction
    public final String getFlash() {
        CameraStandEntity standEntity = tile.getStandEntity();
        if (standEntity != null && !standEntity.getCamera().isEmpty() && standEntity.getCamera().getItem() instanceof CameraItem item) {
            return CameraSettings.FLASH_MODE.get(standEntity.getCamera()).name();
        }
        return null;
    }

    @LuaFunction
    public final boolean fix() {
        CameraStandEntity standEntity = tile.getStandEntity();
        if (standEntity != null) {
            if (standEntity.isMalfunctioned()) {
                ((CameraStandEntityDuck) standEntity).playRepair();
            }
            standEntity.setMalfunctioned(false);
            return true;
        }
        return false;
    }

    @LuaFunction
    public final boolean attachEvents() {
        CameraStandEntity standEntity = tile.getStandEntity();
        if(standEntity != null) {
            tile.events = true;
            return true;
        }
        return false;
    }

    @LuaFunction
    public final void detachEvents() {
        tile.events = false;
    }

    @Override
    public String getType() {
        return "stand_controller";
    }

    @Override
    public boolean equals(@Nullable IPeripheral iPeripheral) {
        return Objects.equals(this, iPeripheral);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (StandControllerPeripheral) obj;
        return Objects.equals(this.tile, that.tile);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tile);
    }

    public void updateRotation(double xRot, double yRot) {
        for (IComputerAccess computer : computers) {
            yRot = yRot % 360;
            if(yRot < 0)
                yRot += 360;
            computer.queueEvent("stand_controller_rotation", computer.getAttachmentName(), yRot % 360, xRot);
        }
    }

    @LuaFunction
    public final boolean getMute() {
        CameraStandEntity standEntity = tile.getStandEntity();
        if (standEntity != null && !standEntity.getCamera().isEmpty()) {
            return standEntity.isSilent();
        }
        return false;
    }

    @LuaFunction
    public final boolean setMute(boolean mute) {
        CameraStandEntity standEntity = tile.getStandEntity();
        if (standEntity != null && !standEntity.getCamera().isEmpty()) {
            standEntity.setSilent(mute);
            return true;
        }
        return false;
    }

    @LuaFunction
    public final Double getCooldown() {
        CameraStandEntity standEntity = tile.getStandEntity();
        if (standEntity != null && !standEntity.getCamera().isEmpty()) {
            if (!(standEntity.getCamera().getItem() instanceof CmosCameraItem)) {return -1d;}
            ItemStack stack = standEntity.getCamera();
            CmosCameraItem camera = (CmosCameraItem)stack.getItem();
            String exposureId = ExposureIdentifier.createId("testing");
            CameraId cameraId = camera.getOrCreateId(stack);
            // the minimum I could get away with here
            CaptureParameters captureParameters = new CaptureParameters.Builder(exposureId)
            .setCameraID(cameraId)
            .setCropFactor(camera.getCropFactor())
            .setFilmProperties(camera.getFilmProperties(stack))
            .build();
            return (double)camera.calculateCooldownAfterShot(stack,captureParameters);
        }
        return -1d;
    }

    @LuaFunction
    public final boolean setFilmProperties(IArguments arg) throws LuaException {
        CameraStandEntity standEntity = tile.getStandEntity();
        ObjectLuaTable newFilmProperties = new ObjectLuaTable(arg.getTable(0));
        if (standEntity != null && !standEntity.getCamera().isEmpty() && standEntity.getCamera().getItem() instanceof CmosCameraItem item) {
            closeCamera();
            ItemStack stack = standEntity.getCamera();
            CmosCameraItem cam = (CmosCameraItem)stack.getItem();
            FilmProperties fp = cam.getFilmProperties(stack);
            ExposureType curExposureType;
            try {
                Field f = FilmProperties.class.getDeclaredField("type");
                f.setAccessible(true);
                curExposureType = (ExposureType)f.get(fp);
            } catch(Exception e) {
                curExposureType = null;
            };
            FilmStyle curFilmStyle;
            try {
                Field f = FilmProperties.class.getDeclaredField("style");
                f.setAccessible(true);
                curFilmStyle = (FilmStyle)f.get(fp);
            } catch(Exception e) {
                curFilmStyle = null;
            }
            Object type = newFilmProperties.get("exposureType");
            Object size = newFilmProperties.get("size");
            Object style = newFilmProperties.get("filmStyle");

            if (size != null) {
                if (!(size instanceof Double)) {
                    throw(new LuaException("size is declared in new film properties but is not a number."));
                }
                fp = fp.withSize(((Double)size).intValue());
            }
            if (type != null) {
                ObjectLuaTable exposureType = new ObjectLuaTable((Map<Object,Object>)type);
                Object name = exposureType.get("name");
                // Object imageColor = exposureType.get("imageColor");
                // Object filmColor = exposureType.get("filmColor");
                final String newName;
                // Color newImageColor = curExposureType.getImageColor();
                // FilmColor newFilmColor = curExposureType.getFilmColor();
                if(name != null) {
                    if(!(name instanceof String)) {
                        throw(new LuaException("name is declared in exposureType of new film properties but is not a string."));
                    }
                    newName = (String)name;
                } else {
                     newName = curExposureType.getSerializedName();
                }
                // I went through all this effort just to find out that these two are enums

                // if(imageColor != null) {
                //     if(!(imageColor instanceof Map<?,?>)) {
                //         throw(new LuaException("imageColor is declared in exposureType of new film properties but is not an argb table."));
                //     }
                //     ObjectLuaTable imageColorTable = new ObjectLuaTable((Map<Object,Object>)imageColor);
                //     try {
                //     newImageColor = new Color(
                //         ((Double)imageColorTable.get("a")).intValue(),
                //         ((Double)imageColorTable.get("r")).intValue(),
                //         ((Double)imageColorTable.get("g")).intValue(),
                //         ((Double)imageColorTable.get("b")).intValue());
                //     } catch(Exception e) {
                //         throw(new LuaException("imageColor is declared in exposureType of new film properties but is not an argb table."));
                //     };
                // }
                // if(filmColor != null) {
                //     if(!(filmColor instanceof Map<?,?>)) {
                //         throw(new LuaException("filmColor is declared in exposureType of new film properties but is not an rgba table."));
                //     }
                //     ObjectLuaTable filmColorTable = new ObjectLuaTable((Map<Object,Object>)filmColor);
                //     try {
                //     newFilmColor = new FilmColor(
                //         ((Double)filmColorTable.get("r")).intValue(),
                //         ((Double)filmColorTable.get("g")).intValue(),
                //         ((Double)filmColorTable.get("b")).intValue(),
                //         ((Double)filmColorTable.get("a")).intValue());
                //     } catch(Exception e) {
                //         throw(new LuaException("filmColor is declared in exposureType of new film properties but is not an rgba table."));
                //     };
                // }
                
                // in the event someone or something adds a new film type rather than just using a switchy case here
                ExposureType newExposureType = Arrays.stream(ExposureType.values()).filter(d -> d.getSerializedName().equalsIgnoreCase(newName)).findAny().orElse(ExposureType.COLOR);
                fp = fp.withType(newExposureType);
            }
            if (style != null) {
                if (!(style instanceof Map)) {
                    throw(new LuaException("filmStyle is declared in new film properties but is not a table."));
                }
                ObjectLuaTable filmStyleTable = new ObjectLuaTable((Map<Object,Object>)style);
                FilmStyle newFilmStyle = curFilmStyle;
                HSB newHSB;
                ColorBalance newColorBalance;
                Float newNoise;
                Object cur = filmStyleTable.get("sensitivity");
                if (cur != null) {
                    if (!(cur instanceof Double)) {
                        throw(new LuaException("sensitivity is declared in new filmStyle but is not a number"));
                    }
                    newFilmStyle = newFilmStyle.withSensitivity(((Double)cur).floatValue());
                }
                cur = filmStyleTable.get("contrast");
                if (cur != null) {
                    if (!(cur instanceof Double)) {
                        throw(new LuaException("contrast is declared in new filmStyle but is not a number"));
                    }
                    newFilmStyle = newFilmStyle.withContrast(((Double)cur).floatValue());
                }
                cur = filmStyleTable.get("levels");
                if (cur != null) {
                    if (!(cur instanceof Map)) {
                        throw(new LuaException("levels is declared in new filmStyle but is not a table"));
                    }
                    // back to reflection hell...
                    ObjectLuaTable levels = new ObjectLuaTable((Map<Object,Object>)cur);
                    Levels curLevels;
                    try {
                        Field levs = FilmStyle.class.getDeclaredField("levels");
                        levs.setAccessible(true);
                        curLevels = (Levels)levs.get(newFilmStyle);
                    } catch(Exception e) {
                        curLevels = null;
                    }
                    int shadows;
                    int midtones;
                    int highlights;
                    int black;
                    int white;
                    try {
                        Object temp = levels.get("shadows");
                        if (temp != null) {
                            if (!(temp instanceof Double)) {
                                throw(new LuaException("shadows is declared in new levels but is not a number"));
                            }
                            shadows = ((Double)temp).intValue();
                        } else {
                            Field f = Levels.class.getDeclaredField("shadows");
                            f.setAccessible(true);
                            shadows = ((Double)f.get(curLevels)).intValue();
                        }
                        temp = levels.get("midtones");
                        if (temp != null) {
                            if (!(temp instanceof Double)) {
                                throw(new LuaException("midtones is declared in new levels but is not a number"));
                            }
                            midtones = ((Double)temp).intValue();
                        } else {
                            Field f = Levels.class.getDeclaredField("midtones");
                            f.setAccessible(true);
                            midtones = ((Double)f.get(curLevels)).intValue();
                        }
                        temp = levels.get("highlights");
                        if (temp != null) {
                            if (!(temp instanceof Double)) {
                                throw(new LuaException("highlights is declared in new levels but is not a number"));
                            }
                            highlights = ((Double)temp).intValue();
                        } else {
                            Field f = Levels.class.getDeclaredField("highlights");
                            f.setAccessible(true);
                            highlights = ((Double)f.get(curLevels)).intValue();
                        }
                        temp = levels.get("black");
                        if (temp != null) {
                            if (!(temp instanceof Double)) {
                                throw(new LuaException("black is declared in new levels but is not a number"));
                            }
                            black = ((Double)temp).intValue();
                        } else {
                            Field f = Levels.class.getDeclaredField("black");
                            f.setAccessible(true);
                            black = ((Double)f.get(curLevels)).intValue();
                        }
                        temp = levels.get("white");
                        if (temp != null) {
                            if (!(temp instanceof Double)) {
                                throw(new LuaException("white is declared in new levels but is not a number"));
                            }
                            white = ((Double)temp).intValue();
                        } else {
                            Field f = Levels.class.getDeclaredField("white");
                            f.setAccessible(true);
                            white = ((Double)f.get(curLevels)).intValue();
                        }
                    } catch(Exception e) {throw((LuaException)e);}
                    newFilmStyle = newFilmStyle.withLevels(new Levels(shadows,midtones,highlights,black,white));
                }
                cur = filmStyleTable.get("hsb");
                if (cur != null) {
                    if (!(cur instanceof Map)) {
                        throw(new LuaException("hsb is declared in new filmStyle but is not a table"));
                    }
                    ObjectLuaTable hsb = new ObjectLuaTable((Map<Object,Object>)cur);
                    HSB curHSB;
                    try {
                        Field hsbf = FilmStyle.class.getDeclaredField("hsb");
                        hsbf.setAccessible(true);
                        curHSB = (HSB)hsbf.get(newFilmStyle);
                    } catch(Exception e) {
                        curHSB = null;
                    }
                    Double h;
                    Double s;
                    Double b;
                    try {
                    Object temp = hsb.get("h");
                    if (temp != null) {
                        if (!(temp instanceof Double)) {
                            throw(new LuaException("h is declared in new hsb but is not a number"));
                        }
                        h = ((Double)temp);
                    } else {
                        Field f = HSB.class.getDeclaredField("h");
                        f.setAccessible(true);
                        h = ((Double)f.get(curHSB));
                    }
                    temp = hsb.get("s");
                    if (temp != null) {
                        if (!(temp instanceof Double)) {
                            throw(new LuaException("s is declared in new hsb but is not a number"));
                        }
                        s = ((Double)temp);
                    } else {
                        Field f = HSB.class.getDeclaredField("s");
                        f.setAccessible(true);
                        s = ((Double)f.get(curHSB));
                    }
                    temp = hsb.get("b");
                    if (temp != null) {
                        if (!(temp instanceof Double)) {
                            throw(new LuaException("b is declared in new levels but is not a number"));
                        }
                        b = ((Double)temp);
                    } else {
                        Field f = HSB.class.getDeclaredField("b");
                        f.setAccessible(true);
                        b = ((Double)f.get(curHSB));
                    }
                    newFilmStyle = newFilmStyle.withHSB(new HSB(h.floatValue(),s.floatValue(),b.floatValue()));
                    } catch(Exception e) {throw((LuaException)e);}
                }
                cur = filmStyleTable.get("hsb");
                if (cur != null) {
                    if (!(cur instanceof Map)) {
                        throw(new LuaException("hsb is declared in new filmStyle but is not a table"));
                    }
                    ObjectLuaTable hsb = new ObjectLuaTable((Map<Object,Object>)cur);
                    HSB curHSB;
                    try {
                        Field hsbf = FilmStyle.class.getDeclaredField("hsb");
                        hsbf.setAccessible(true);
                        curHSB = (HSB)hsbf.get(newFilmStyle);
                    } catch(Exception e) {
                        curHSB = null;
                    }
                    Double h;
                    Double s;
                    Double b;
                    try {
                    Object temp = hsb.get("h");
                    if (temp != null) {
                        if (!(temp instanceof Double)) {
                            throw(new LuaException("h is declared in new hsb but is not a number"));
                        }
                        h = ((Double)temp);
                    } else {
                        Field f = HSB.class.getDeclaredField("hue");
                        f.setAccessible(true);
                        h = ((Double)f.get(curHSB));
                    }
                    temp = hsb.get("s");
                    if (temp != null) {
                        if (!(temp instanceof Double)) {
                            throw(new LuaException("s is declared in new hsb but is not a number"));
                        }
                        s = ((Double)temp);
                    } else {
                        Field f = HSB.class.getDeclaredField("saturation");
                        f.setAccessible(true);
                        s = ((Double)f.get(curHSB));
                    }
                    temp = hsb.get("b");
                    if (temp != null) {
                        if (!(temp instanceof Double)) {
                            throw(new LuaException("b is declared in new hsb but is not a number"));
                        }
                        b = ((Double)temp);
                    } else {
                        Field f = HSB.class.getDeclaredField("brightness");
                        f.setAccessible(true);
                        b = ((Double)f.get(curHSB));
                    }
                    } catch(Exception e) {throw((LuaException)e);}
                    newFilmStyle = newFilmStyle.withHSB(new HSB(h.floatValue(),s.floatValue(),b.floatValue()));
                }
                cur = filmStyleTable.get("colorBalance");
                if (cur != null) {
                    if (!(cur instanceof Map)) {
                        throw(new LuaException("hsb is declared in new filmStyle but is not a table"));
                    }
                    ObjectLuaTable colorbalance = new ObjectLuaTable((Map<Object,Object>)cur);
                    ColorBalance curColorBalance;
                    try {
                        Field cbt = FilmStyle.class.getDeclaredField("hsb");
                        cbt.setAccessible(true);
                        curColorBalance = (ColorBalance)cbt.get(newFilmStyle);
                    } catch(Exception e) {
                        curColorBalance = null;
                    }
                    Double r;
                    Double g;
                    Double b;
                    try {
                    Object temp = colorbalance.get("r");
                    if (temp != null) {
                        if (!(temp instanceof Double)) {
                            throw(new LuaException("r is declared in new colorBalance but is not a number"));
                        }
                        r = ((Double)temp);
                    } else {
                        Field f = ColorBalance.class.getDeclaredField("r");
                        f.setAccessible(true);
                        r = (Double)f.get(curColorBalance);
                    }
                    temp = colorbalance.get("g");
                    if (temp != null) {
                        if (!(temp instanceof Double)) {
                            throw(new LuaException("g is declared in colorBalance hsb but is not a number"));
                        }
                        g = (Double)temp;
                    } else {
                        Field f = ColorBalance.class.getDeclaredField("g");
                        f.setAccessible(true);
                        g = (Double)f.get(curColorBalance);
                    }
                    temp = colorbalance.get("b");
                    if (temp != null) {
                        if (!(temp instanceof Double)) {
                            throw(new LuaException("b is declared in new colorBalance but is not a number"));
                        }
                        b = (Double)temp;
                    } else {
                        Field f = ColorBalance.class.getDeclaredField("b");
                        f.setAccessible(true);
                        b = (Double)f.get(curColorBalance);
                    }
                    newFilmStyle = newFilmStyle.withColorBalance(new ColorBalance(r.floatValue(),g.floatValue(),b.floatValue()));
                    } catch(Exception e) {throw((LuaException)e);}
                }
                cur = filmStyleTable.get("noise");
                if (cur != null) {
                    if (!(cur instanceof Double)) {
                        throw(new LuaException("noise is declared in new filmStyle but is not a number"));
                    }
                    newFilmStyle = newFilmStyle.withNoise(((Double)cur).floatValue());
                }
                fp = fp.withStyle(newFilmStyle);
            }

            cam.setFilmProperties(stack,fp);
            // size (optional int)

            // colorPalette (resource key, this is a non fucking starter un fucking fortunately)

            // dither mode (string)

            // style (go deeper)
            // Float contrast
            // Levels levels
            // HSB hsb
            // ColorBalance colorBalance
            // Float noise
            return true;
        }
            return false;
    }

    @LuaFunction
    public final Object getFilmProperties() {
        CameraStandEntity standEntity = tile.getStandEntity();
        if (standEntity != null && !standEntity.getCamera().isEmpty() && standEntity.getCamera().getItem() instanceof CameraItem item) {
            closeCamera();
            CameraItem cam = (CameraItem)standEntity.getCamera().getItem();
            FilmProperties fp = cam.getFilmProperties(standEntity.getCamera());
            FilmStyle fstyle;
            ExposureType etype;
            Map<Object,Object> retvalue = new HashMap();
            try {
                Field f = FilmProperties.class.getDeclaredField("style");
                f.setAccessible(true);
                fstyle = (FilmStyle)f.get(fp);
            } catch(Exception e) {
                fstyle = null;
                retvalue.put("fstyle_err",e.toString());
            };
            try {
                Field f = FilmProperties.class.getDeclaredField("type");
                f.setAccessible(true);
                etype = (ExposureType)f.get(fp);
            } catch(Exception e) {
                etype = null;
                retvalue.put("etype_err",e.toString());
            };

            // size (optional int)
            
            // update: forge lets you do a thing where you can register a new registry so maybe
            // just maybe I can network colorPalettes
            
            // colorPalette (resource key, this is a non fucking starter un fucking fortunately)

            // dither mode (string)

            // style (go deeper)
            // Float contrast
            // Levels levels
            // HSB hsb
            // ColorBalance colorBalance
            // Float noise
            retvalue.put("size",fp.getSize());
            try {
                {
                    Field f = FilmProperties.class.getDeclaredField("colorPalette");
                    f.setAccessible(true);
                    retvalue.put("colorPalette",((ResourceKey<ColorPalette>)(f.get(fp))).toString());
                }
                {
                    Field f = FilmProperties.class.getDeclaredField("ditherMode");
                    f.setAccessible(true);
                    retvalue.put("ditherMode",(f.get(fp)).toString());
                }
                Map<Object,Object> filmStyle = new HashMap();
                retvalue.put("filmStyle",filmStyle);
                {
                    Field f = FilmStyle.class.getDeclaredField("contrast");
                    f.setAccessible(true);
                    filmStyle.put("contrast",((Float)f.get(fstyle)).doubleValue());
                }
                {
                    Field f = FilmStyle.class.getDeclaredField("levels");
                    f.setAccessible(true);
                    Levels l = (Levels)f.get(fstyle);
                    Map<Object, Object> levels = new HashMap();
                    filmStyle.put("levels",levels);
                    Field fshadows     = Levels.class.getDeclaredField("shadows");
                    Field fmidtones   = Levels.class.getDeclaredField("midtones");
                    Field fhighlights = Levels.class.getDeclaredField("highlights");
                    Field fblack      = Levels.class.getDeclaredField("black");
                    Field fwhite      = Levels.class.getDeclaredField("white");
                    fshadows.setAccessible(true);
                    fmidtones.setAccessible(true);
                    fhighlights.setAccessible(true);
                    fblack.setAccessible(true);
                    fwhite.setAccessible(true);
                    levels.put("shadows",fshadows.get(l));
                    levels.put("midtones",fmidtones.get(l));
                    levels.put("highlights",fhighlights.get(l));
                    levels.put("black",fblack.get(l));
                    levels.put("white",fwhite.get(l));
                }
                {
                    Field f = FilmStyle.class.getDeclaredField("hsb");
                    f.setAccessible(true);
                    HSB hsb = (HSB)f.get(fstyle);
                    Map<Object,Object> hsbl = new HashMap();
                    filmStyle.put("hsb",hsbl);
                    Field h = HSB.class.getDeclaredField("hue");
                    Field s = HSB.class.getDeclaredField("saturation");
                    Field b = HSB.class.getDeclaredField("brightness");
                    h.setAccessible(true);
                    s.setAccessible(true);
                    b.setAccessible(true);
                    hsbl.put("h",((Float)h.get(hsb)).doubleValue());
                    hsbl.put("s",((Float)s.get(hsb)).doubleValue());
                    hsbl.put("b",((Float)b.get(hsb)).doubleValue());
                }
                {
                    Field f = FilmStyle.class.getDeclaredField("colorBalance");
                    f.setAccessible(true);
                    ColorBalance cb = (ColorBalance)f.get(fstyle);
                    Map<Object,Object> cbl = new HashMap();
                    filmStyle.put("colorBalance",cbl);
                    Field r = ColorBalance.class.getDeclaredField("r");
                    Field g = ColorBalance.class.getDeclaredField("g");
                    Field b = ColorBalance.class.getDeclaredField("b");
                    r.setAccessible(true);
                    g.setAccessible(true);
                    b.setAccessible(true);
                    cbl.put("r",((Float)r.get(cb)).doubleValue());
                    cbl.put("g",((Float)g.get(cb)).doubleValue());
                    cbl.put("b",((Float)b.get(cb)).doubleValue());
                }
                {
                    Field f = FilmStyle.class.getDeclaredField("noise");
                    f.setAccessible(true);
                    filmStyle.put("noise",((Float)(f.get(fstyle))).doubleValue());
                }
                Map<Object, Object> exposureType = new HashMap();
                retvalue.put("exposureType",exposureType);
                exposureType.put("name",etype.getSerializedName());
                Map<Object, Object> icolor = new HashMap();
                Color imageColor = etype.getImageColor();
                icolor.put("a",imageColor.getA());
                icolor.put("r",imageColor.getR());
                icolor.put("g",imageColor.getG());
                icolor.put("b",imageColor.getB());
                exposureType.put("imageColor",icolor);
                FilmColor filmColor = etype.getFilmColor();
                Map<Object, Object> fcolor = new HashMap();
                {
                    Field r = FilmColor.class.getDeclaredField("r");
                    Field g = FilmColor.class.getDeclaredField("g");
                    Field b = FilmColor.class.getDeclaredField("b");
                    Field a = FilmColor.class.getDeclaredField("a");
                    r.setAccessible(true);
                    g.setAccessible(true);
                    b.setAccessible(true);
                    a.setAccessible(true);
                    fcolor.put("r",((Float)r.get(filmColor)).doubleValue());
                    fcolor.put("g",((Float)g.get(filmColor)).doubleValue());
                    fcolor.put("b",((Float)b.get(filmColor)).doubleValue());
                    fcolor.put("a",((Float)a.get(filmColor)).doubleValue());
                }
                exposureType.put("filmColor",fcolor);
            } catch(Exception e) {
                retvalue.put("err",e.toString());
            }
            return retvalue;
        }
            return false;
    }
}
