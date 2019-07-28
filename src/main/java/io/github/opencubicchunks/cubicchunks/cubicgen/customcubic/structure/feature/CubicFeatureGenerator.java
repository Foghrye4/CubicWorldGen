/*
 *  This file is part of Cubic World Generation, licensed under the MIT License (MIT).
 *
 *  Copyright (c) 2015 contributors
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 */
package io.github.opencubicchunks.cubicchunks.cubicgen.customcubic.structure.feature;

import static io.github.opencubicchunks.cubicchunks.api.util.Coords.cubeToCenterBlock;

import io.github.opencubicchunks.cubicchunks.api.worldgen.CubePrimer;
import io.github.opencubicchunks.cubicchunks.api.worldgen.CubicStructureGenerator;
import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.api.util.XYZMap;
import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ReportedException;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.MapGenStructureIO;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.gen.structure.StructureComponent;
import net.minecraft.world.gen.structure.StructureStart;
import net.minecraftforge.common.util.Constants;

import java.util.Random;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public abstract class CubicFeatureGenerator extends CubicStructureGenerator {

    private CubicFeatureData structureData;

    /**
     * Used to store a list of all structures that have been recursively generated. Used so that during recursive
     * generation, the structure generator can avoid generating structures that intersect ones that have already been
     * placed.
     */
    protected XYZMap<ICubicStructureStart> structureMap = new XYZMap<>(0.5f, 1024);

    /**
     * @param spacing The minimum spacing. Structures thataren't generated at integer multiple coords of this value will be skipped.
     */
    protected CubicFeatureGenerator(int spacing) {
        super(spacing);
    }

    public abstract String getStructureName();

    @SuppressWarnings("ConstantConditions")
    @Override public void generate(World world, @Nullable CubePrimer cube, CubePos cubePos) {
        super.generate(world, cube, cubePos);
    }

    @Override
    protected synchronized void generate(World world, @Nullable CubePrimer cube, int structureX, int structureY, int structureZ,
            CubePos generatedCubePos) {
        this.initializeStructureData((World) world);

        if (!this.structureMap.contains(structureX, structureY, structureZ)) {
            this.rand.nextInt();
            try {
                if (this.canSpawnStructureAtCoords(structureX, structureY, structureZ)) {
                    StructureStart start = this.getStructureStart(structureX, structureY, structureZ);
                    this.structureMap.put((ICubicStructureStart) start);
                    if (start.isSizeableStructure()) {
                        this.setStructureStart(structureX, structureY, structureZ, start);
                    }
                }
            } catch (Throwable throwable) {
                CrashReport report = CrashReport.makeCrashReport(throwable, "Exception preparing structure feature");
                CrashReportCategory category = report.makeCategory("Feature being prepared");
                category.addDetail("Is feature chunk", () -> this.canSpawnStructureAtCoords(structureX, structureY, structureZ) ? "True" : "False");
                category.addCrashSection("Chunk location", String.format("%d,%d,%d", structureX, structureY, structureZ));
                category.addDetail("Structure type", () -> this.getClass().getCanonicalName());
                throw new ReportedException(report);
            }
        }
    }

    public synchronized boolean generateStructure(World world, Random rand, CubePos cubePos) {
        this.initializeStructureData(world);
        int centerX = cubeToCenterBlock(cubePos.getX());
        int centerY = cubeToCenterBlock(cubePos.getY());
        int centerZ = cubeToCenterBlock(cubePos.getZ());
        boolean generated = false;
        for (ICubicStructureStart cubicStructureStart : this.structureMap) {
            StructureStart structStart = (StructureStart) cubicStructureStart;
            // TODO: cubic chunks version of isValidForPostProcess and notifyPostProcess (mixin)
            if (structStart.isSizeableStructure() && structStart.isValidForPostProcess(cubePos.chunkPos())
                    && structStart.getBoundingBox().intersectsWith(
                    new StructureBoundingBox(centerX, centerY, centerZ, centerX + ICube.SIZE - 1, centerY + ICube.SIZE - 1, centerZ + ICube.SIZE - 1))) {
                structStart.generateStructure(world, rand,
                        new StructureBoundingBox(centerX, centerY, centerZ, centerX + ICube.SIZE - 1, centerY + ICube.SIZE - 1, centerZ + ICube.SIZE - 1));
                structStart.notifyPostProcessAt(cubePos.chunkPos());
                generated = true;
                this.setStructureStart(structStart.getChunkPosX(), cubicStructureStart.getChunkPosY(), structStart.getChunkPosZ(), structStart);
            }
        }

        return generated;
    }

    public boolean isInsideStructure(BlockPos pos) {
        this.initializeStructureData((World) this.world);
        return this.getStructureAt(pos) != null;
    }

    @Nullable
    protected StructureStart getStructureAt(BlockPos pos) {
        for (ICubicStructureStart cubicStructureStart : this.structureMap) {
            StructureStart start = (StructureStart) cubicStructureStart;

            if (start.isSizeableStructure() && start.getBoundingBox().isVecInside(pos)) {
                for (StructureComponent component : start.getComponents()) {
                    if (component.getBoundingBox().isVecInside(pos)) {
                        return start;
                    }
                }
            }
        }
        return null;
    }

    public boolean isPositionInStructure(World world, BlockPos pos) {
        this.initializeStructureData(world);
        for (ICubicStructureStart cubicStart : this.structureMap) {
            StructureStart start = (StructureStart) cubicStart;
            if (start.isSizeableStructure() && start.getBoundingBox().isVecInside(pos)) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    public abstract BlockPos getClosestStrongholdPos(World worldIn, BlockPos pos, boolean findUnexplored);

    protected void initializeStructureData(World world) {
        if (this.structureData != null) {
            return;
        }
        this.structureData = (CubicFeatureData) world.getPerWorldStorage().getOrLoadData(CubicFeatureData.class, this.getStructureName());

        if (this.structureData == null) {
            this.structureData = new CubicFeatureData(this.getStructureName());
            world.getPerWorldStorage().setData(this.getStructureName(), this.structureData);
        } else {
            NBTTagCompound nbttagcompound = this.structureData.getTagCompound();

            for (String s : nbttagcompound.getKeySet()) {
                NBTBase nbtbase = nbttagcompound.getTag(s);

                if (nbtbase.getId() == Constants.NBT.TAG_COMPOUND) {
                    NBTTagCompound tag = (NBTTagCompound) nbtbase;

                    if (tag.hasKey("ChunkX") && tag.hasKey("ChunkY") && tag.hasKey("ChunkZ")) {
                        StructureStart structurestart = MapGenStructureIO.getStructureStart(tag, world);

                        if (structurestart != null) {
                            this.structureMap.put((ICubicStructureStart) structurestart);
                        }
                    }
                }
            }
        }
    }

    private void setStructureStart(int chunkX, int chunkY, int chunkZ, StructureStart start) {
        this.structureData.writeInstance(start.writeStructureComponentsToNBT(chunkX, chunkZ), chunkX, chunkY, chunkZ);
        this.structureData.markDirty();
    }

    protected abstract boolean canSpawnStructureAtCoords(int chunkX, int chunkY, int chunkZ);

    protected abstract StructureStart getStructureStart(int chunkX, int chunkY, int chunkZ);
}
