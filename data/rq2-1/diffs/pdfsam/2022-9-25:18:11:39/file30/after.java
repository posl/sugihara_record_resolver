/* 
 * This file is part of the PDF Split And Merge source code
 * Created on 26/giu/2014
 * Copyright 2017 by Sober Lemur S.a.s. di Vacondio Andrea (info@pdfsam.org).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as 
 * published by the Free Software Foundation, either version 3 of the 
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.pdfsam.core.support.params;

import org.sejda.model.input.PdfFileSource;
import org.sejda.model.output.SingleOrMultipleTaskOutput;
import org.sejda.model.parameter.base.MultiplePdfSourceMultipleOutputParameters;
import org.sejda.model.parameter.base.SinglePdfSourceMultipleOutputParameters;

/**
 * Abstract builder for {@link SinglePdfSourceMultipleOutputParameters}.
 * 
 * @author Andrea Vacondio
 * @param <P>
 *            type of the parameters the builder builds
 */
public abstract class SinglePdfSourceMultipleOutputParametersBuilder<P extends MultiplePdfSourceMultipleOutputParameters>
        extends AbstractPdfOutputParametersBuilder<P>
        implements SinglePdfSourceTaskParametersBuilder<P>, MultipleOutputTaskParametersBuilder<P> {

    private PdfFileSource source;
    private SingleOrMultipleTaskOutput output;
    private String prefix;

    @Override
    public void source(PdfFileSource source) {
        this.source = source;
    }

    @Override
    public void output(SingleOrMultipleTaskOutput output) {
        this.output = output;
    }

    @Override
    public void prefix(String prefix) {
        this.prefix = prefix;
    }

    protected PdfFileSource getSource() {
        return source;
    }

    protected SingleOrMultipleTaskOutput getOutput() {
        return output;
    }

    protected String getPrefix() {
        return prefix;
    }

}
