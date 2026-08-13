package plume.summoner.pinin.elements;

import plume.summoner.pinin.utils.IndexSet;

/**
 * Author: Towdium
 * Date: 21/04/19
 */
public interface Element {
    IndexSet match(String str, int start, boolean partial);
}
