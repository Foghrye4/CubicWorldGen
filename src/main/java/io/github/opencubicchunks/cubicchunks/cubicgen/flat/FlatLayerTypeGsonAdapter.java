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
package io.github.opencubicchunks.cubicchunks.cubicgen.flat;

import java.io.IOException;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTException;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.biome.Biome;

public class FlatLayerTypeGsonAdapter extends TypeAdapter<Layer> {

    @Override
    public void write(JsonWriter out, Layer value) throws IOException {
        System.out.println("writing biome" + value.biome);
        out.beginObject();
        out.name("fromY");
        out.value(value.fromY);
        out.name("toY");
        out.value(value.toY);
        out.name("blockstate");
        NBTTagCompound tag = new NBTTagCompound();
        NBTUtil.writeBlockState(tag, value.blockState);
        out.value(tag.toString());
        if (value.biome != null) {
            out.name("biome");
            out.value(value.biome.getRegistryName().toString());
        }
        out.endObject();
    }

    @Override
    public Layer read(JsonReader reader) throws IOException {
        reader.beginObject();
        int fromY = Integer.MIN_VALUE;
        int toY = Integer.MAX_VALUE;
        IBlockState blockState = Blocks.STONE.getDefaultState();
        Biome biome = null;
        while (reader.hasNext()) {
            String name = reader.nextName();
            if (name.equals("fromY"))
                fromY = reader.nextInt();
            else if (name.equals("toY"))
                toY = reader.nextInt();
            else if (name.equals("blockstate")) {
                NBTTagCompound tag;
                try {
                    tag = JsonToNBT.getTagFromJson(reader.nextString());
                    blockState = NBTUtil.readBlockState(tag);
                } catch (NBTException e) {
                    e.printStackTrace();
                }
            } else if (name.equals("biome")) {
                biome = Biome.REGISTRY.getObject(new ResourceLocation(reader.nextString()));
                System.out.println("reading biome" + biome);
            } else {
                reader.skipValue();
            }
        }
        Layer layer = new Layer(fromY, toY, biome, blockState);
        reader.endObject();
        return layer;
    }
}
