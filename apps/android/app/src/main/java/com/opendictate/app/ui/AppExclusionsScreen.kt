package com.opendictate.app.ui

import android.widget.ImageView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.opendictate.app.R

@Composable
internal fun AppExclusionsScreen(
    apps: List<InstalledApp>,
    excludedPackages: Set<String>,
    loading: Boolean,
    onBack: () -> Unit,
    onExcludedChange: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val visibleApps = filterAndPrioritizeApps(apps, query, excludedPackages)
    val packageManager = LocalContext.current.packageManager

    Column(modifier.fillMaxSize().background(Ink).statusBarsPadding().navigationBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 8.dp, end = 20.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.app_exclusions_back))
            }
            Spacer(Modifier.width(8.dp))
            Text(
                stringResource(R.string.app_exclusions_title),
                color = White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 20.sp,
            )
        }
        Text(
            stringResource(R.string.app_exclusions_description),
            color = Fog,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
        )
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            label = { Text(stringResource(R.string.app_exclusions_search)) },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            trailingIcon = if (query.isNotEmpty()) {
                {
                    IconButton(onClick = { query = "" }) {
                        Icon(Icons.Outlined.Close, stringResource(R.string.app_exclusions_clear_search))
                    }
                }
            } else null,
            singleLine = true,
        )
        Spacer(Modifier.height(12.dp))
        if (loading) {
            CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
        } else if (visibleApps.isEmpty()) {
            Text(
                stringResource(if (apps.isEmpty()) R.string.app_exclusions_empty else R.string.app_exclusions_no_matches),
                color = Fog,
                modifier = Modifier.padding(20.dp),
            )
        } else {
            LazyColumn(contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 20.dp)) {
                items(visibleApps, key = InstalledApp::packageName) { app ->
                    val excluded = app.packageName in excludedPackages
                    Row(
                        Modifier.fillMaxWidth().clickable {
                            onExcludedChange(app.packageName, !excluded)
                        }.padding(horizontal = 20.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AndroidView(
                            factory = { context -> ImageView(context).apply { scaleType = ImageView.ScaleType.FIT_CENTER } },
                            update = { image ->
                                image.setImageDrawable(runCatching {
                                    packageManager.getApplicationIcon(app.packageName)
                                }.getOrNull())
                            },
                            modifier = Modifier.size(40.dp),
                        )
                        Text(
                            app.label,
                            modifier = Modifier.weight(1f),
                            color = White,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Switch(
                            checked = excluded,
                            onCheckedChange = { onExcludedChange(app.packageName, it) },
                            modifier = Modifier,
                        )
                    }
                }
            }
        }
    }
}

internal fun filterAndPrioritizeApps(
    apps: List<InstalledApp>,
    query: String,
    excludedPackages: Set<String>,
): List<InstalledApp> {
    val matching = apps.filter { it.label.contains(query.trim(), ignoreCase = true) }
    val (excluded, included) = matching.partition { it.packageName in excludedPackages }
    return excluded + included
}
