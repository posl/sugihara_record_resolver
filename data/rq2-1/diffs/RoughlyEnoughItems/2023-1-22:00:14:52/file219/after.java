/*
 * This file is licensed under the MIT License, part of Roughly Enough Items.
 * Copyright (c) 2018, 2019, 2020, 2021, 2022, 2023 shedaniel
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package me.shedaniel.rei.plugin.common.displays;

import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.basic.BasicDisplay;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.plugin.common.BuiltinPlugin;
import org.jetbrains.annotations.ApiStatus;

import java.util.Collections;
import java.util.List;

@ApiStatus.Experimental
public class DefaultOxidationScrapingDisplay extends BasicDisplay {
    public DefaultOxidationScrapingDisplay(EntryStack<?> in, EntryStack<?> out) {
        this(Collections.singletonList(EntryIngredient.of(in)), Collections.singletonList(EntryIngredient.of(out)));
    }
    
    public DefaultOxidationScrapingDisplay(List<EntryIngredient> inputs, List<EntryIngredient> outputs) {
        super(inputs, outputs);
    }
    
    public final EntryIngredient getIn() {
        return getInputEntries().get(0);
    }
    
    public final EntryIngredient getOut() {
        return getOutputEntries().get(0);
    }
    
    @Override
    public CategoryIdentifier<?> getCategoryIdentifier() {
        return BuiltinPlugin.OXIDATION_SCRAPING;
    }
    
    public static Serializer<DefaultOxidationScrapingDisplay> serializer() {
        return Serializer.ofSimpleRecipeLess(DefaultOxidationScrapingDisplay::new);
    }
}
