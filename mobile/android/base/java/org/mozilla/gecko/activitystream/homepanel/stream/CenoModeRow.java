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

        final View learnMoreLink = itemView.findViewById(R.id.learn_more_link);
        // TODO: Narrow down to `TextView` and set a different text
        // depending on public/private home page.
        learnMoreLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                final Context context = GeckoAppShell.getApplicationContext();
                Tabs.getInstance().loadUrl(context.getString(R.string.ceno_home_mode_link));
            }
        });
    }
}
