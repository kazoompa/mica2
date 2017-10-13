/*
 * Copyright (c) 2017 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.spi.search;

/**
 * Type of results being searched.
 */
public enum QueryMode {
  SEARCH,  // search for documents (with results and aggregations)
  LIST,    // list documents (with results but no aggregations)
  COVERAGE // search for documents coverage of classifications (with aggregations but no results)
}