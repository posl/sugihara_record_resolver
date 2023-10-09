/* 
 * This file is part of the PDF Split And Merge source code
 * Created on 23 nov 2016
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
package org.pdfsam.premium;

import static org.pdfsam.support.io.NetUtils.urlToStream;
import static org.sejda.commons.util.RequireUtils.requireNotNullArg;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.pdfsam.ConfigurableProperty;
import org.pdfsam.Pdfsam;
import org.pdfsam.i18n.I18nContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.jr.ob.JSON;
import com.fasterxml.jackson.jr.ob.JSON.Feature;

/**
 * @author Andrea Vacondio
 *
 */
public class DefaultPremiumModulesService implements PremiumModulesService {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultPremiumModulesService.class);
    private Pdfsam pdfsam;
    private JSON jackson = new JSON().without(Feature.USE_FIELDS).with(Feature.READ_ONLY, true);

    @Inject
    DefaultPremiumModulesService(Pdfsam pdfsam) {
        requireNotNullArg(pdfsam, "Application info cannot be null");
        this.pdfsam = pdfsam;
    }

    @Override
    public List<PremiumModule> getPremiumModules() {
        try {
            return jackson.listOfFrom(
                    PremiumModule.class,
                    urlToStream(new URL(pdfsam.property(ConfigurableProperty.PREMIUM_MODULES_URL))));
        } catch (IOException e) {
            LOG.warn(I18nContext.getInstance().i18n("Unable to retrieve premium features description"), e);
        }
        return Collections.emptyList();
    }

}
