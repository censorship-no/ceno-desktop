/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.activitystream.homepanel.stream;

import android.content.Context;
import android.support.annotation.LayoutRes;
import android.view.View;
import org.mozilla.gecko.GeckoAppShell;
import org.mozilla.gecko.R;
import org.mozilla.gecko.Tabs;

public class CenoModeRow extends StreamViewHolder {

    public static final @LayoutRes int LAYOUT_ID = R.layout.activity_stream_ceno_mode_row;

    public CenoModeRow(final View itemView) {
        super(itemView);

        final View cenoModeLink = itemView.findViewById(R.id.ceno_home_mode_row);
        // It would be nice to narrow down to `TextView` and set a different text
        // depending on public/private home page,
        // but the row is not created every time;
        // actually I could not find any event that is reliably triggered
        // every time that the view is shown (including creating a new tab of a different mode).
        cenoModeLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                final Context context = GeckoAppShell.getApplicationContext();
                Tabs.getInstance().loadUrl(context.getString(R.string.ceno_mode_manual_link));
            }
        });
    }
}
