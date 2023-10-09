/*
 * This file is part of the PDF Split And Merge source code
 * Created on 23 ott 2015
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
package org.pdfsam.model.news;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Model for a single piece of news
 *
 * @author Andrea Vacondio
 */
public record NewsData(int id, String title, String content, String link, LocalDate date, boolean important) {
    public NewsData(int id, String title, String content, String link, String date, boolean important) {
        this(id, title, content, link, LocalDate.parse(date, DateTimeFormatter.BASIC_ISO_DATE), important);
    }
}
