package com.nsdev.lettergenerator.theme;

import com.nsdev.lettergenerator.model.layout.Keep;
import com.nsdev.lettergenerator.model.table.CellAppearance;

/**
 * Table appearance defaults shared by every authoring API. Explicit table and cell settings win.
 *
 * @param cells defaults for every cell
 * @param header additional repeating-header defaults
 * @param rowKeepTogether default row pagination policy
 */
public record TableAppearance(CellAppearance cells, CellAppearance header, Keep rowKeepTogether) {}
