/*
 * Copyright 2017 by Edi Weissmann (edi.weissmann@gmail.com).
 * 
 * This file is part of the Sejda source code
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.sejda.model.input;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.sejda.model.PageOrientation;
import org.sejda.model.PageSize;

/**
 * Image input file for the merge task. Will be transformed to a PDF on the fly, then used as a PDF merge input.
 * 
 */
public class ImageMergeInput implements MergeInput {

    @NotNull
    @Valid
    private Source<?> source;
    @NotNull
    private PageSize pageSize = PageSize.A4;
    private boolean shouldPageSizeMatchImageSize = false;
    private PageOrientation pageOrientation = PageOrientation.AUTO;

    public ImageMergeInput(Source<?> source) {
        this.source = source;
    }

    public Source<?> getSource() {
        return source;
    }

    public PageSize getPageSize() {
        return pageSize;
    }

    /**
     * sets the default size of the PDF pages generated from images
     * 
     * @param pageSize
     */
    public void setPageSize(PageSize pageSize) {
        this.pageSize = pageSize;
    }

    public boolean isShouldPageSizeMatchImageSize() {
        return shouldPageSizeMatchImageSize;
    }

    /**
     * If true a PDF page generated by an image will have the same size as the image itself
     * 
     * @param shouldPageSizeMatchImageSize
     */
    public void setShouldPageSizeMatchImageSize(boolean shouldPageSizeMatchImageSize) {
        this.shouldPageSizeMatchImageSize = shouldPageSizeMatchImageSize;
    }

    public PageOrientation getPageOrientation() {
        return pageOrientation;
    }

    public void setPageOrientation(PageOrientation pageOrientation) {
        this.pageOrientation = pageOrientation;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append(source).append(pageSize).append(shouldPageSizeMatchImageSize)
                .append(pageOrientation).toString();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(source).append(pageSize).append(shouldPageSizeMatchImageSize)
                .append(pageOrientation).toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ImageMergeInput input)) {
            return false;
        }
        return new EqualsBuilder().append(source, input.getSource()).append(pageOrientation, input.pageOrientation)
                .append(pageSize, input.pageSize)
                .append(shouldPageSizeMatchImageSize, input.shouldPageSizeMatchImageSize).isEquals();
    }

}
