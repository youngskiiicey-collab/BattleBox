package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberCyan

data class BattleBoxPhotoItem(
    val id: String,
    @param:DrawableRes val drawableRes: Int,
    val title: String,
    val subtitle: String,
    val badge: String
)

val battleBoxClubGalleryPhotos = listOf(
    BattleBoxPhotoItem(
        id = "dudhnoi_exterior_sign",
        drawableRes = R.drawable.img_fff_exterior_sign_1794638873184,
        title = "Battle Box : FFF Facade",
        subtitle = "Fight For Fitness signage • Dudhnoi, Assam 783124",
        badge = "EXTERIOR"
    ),
    BattleBoxPhotoItem(
        id = "dudhnoi_crest_wall",
        drawableRes = R.drawable.img_maps_battlebox_wall,
        title = "Winged Crest & Brick Mural",
        subtitle = "Battle Box For Fitness mural • XQPM+99H, Dudhnoi",
        badge = "WALL MURAL"
    ),
    BattleBoxPhotoItem(
        id = "dudhnoi_dumbbell_rack",
        drawableRes = R.drawable.img_fff_dumbbell_rack_1794638872867,
        title = "Jerai Dumbbell & Free Weights",
        subtitle = "Jerai Round Rubber Dumbbells (2.5kg–40kg) & EZ/Olympic bars",
        badge = "FREE WEIGHTS"
    ),
    BattleBoxPhotoItem(
        id = "dudhnoi_smith_rack",
        drawableRes = R.drawable.img_fff_smith_rack_1794638874023,
        title = "Jerai Smith Rack & Cable Tower",
        subtitle = "Smith machine w/ 5kg–25kg bumper plates & Dual Cable Crossover",
        badge = "RACKS & CABLES"
    ),
    BattleBoxPhotoItem(
        id = "dudhnoi_leg_press_floor",
        drawableRes = R.drawable.img_fff_leg_press_floor_1794638874436,
        title = "45° Leg Press & Selectorized Line",
        subtitle = "Jerai plate-loaded 45° leg press, extension/curl & press stations",
        badge = "STRENGTH FLOOR"
    ),
    BattleBoxPhotoItem(
        id = "dudhnoi_neon_mirror_zone",
        drawableRes = R.drawable.img_fff_neon_mirror_zone_1794638874891,
        title = "Neon Wall & Reception Lounge",
        subtitle = "Certified trainer wall, digital HUD clock & recovery seating",
        badge = "CLUB LOUNGE"
    ),
    BattleBoxPhotoItem(
        id = "dudhnoi_treadmill_lounge",
        drawableRes = R.drawable.img_fff_treadmill_lounge_1794638875424,
        title = "Aerofit Commercial Cardio Zone",
        subtitle = "Commercial incline treadmills, cycle bay & open functional turf",
        badge = "CARDIO & TURF"
    ),
    BattleBoxPhotoItem(
        id = "dudhnoi_training_floor",
        drawableRes = R.drawable.img_hero_gym,
        title = "Combat & Turf Conditioning",
        subtitle = "Warm-up sprints, sled/agility lane & HIIT conditioning deck",
        badge = "CONDITIONING"
    )
)

@Composable
fun MapsPhotoShowcaseCard(
    mapsUrl: String,
    modifier: Modifier = Modifier,
    compactHeader: Boolean = false
) {
    val context = LocalContext.current
    var selectedPhoto by remember { mutableStateOf<BattleBoxPhotoItem?>(null) }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, CyberBorder.copy(alpha = 0.7f)),
        shape = MaterialTheme.shapes.large,
        modifier = modifier
            .fillMaxWidth()
            .testTag("maps_photo_showcase_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = null,
                        tint = CyberCyan
                    )
                    Column {
                        Text(
                            text = if (compactHeader) "DUDHNOI FFF CLUB GALLERY" else "DUDHNOI FFF CLUB GALLERY (${battleBoxClubGalleryPhotos.size} PHOTOS)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Battle Box : FFF • Google Maps verified gallery",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                TextButton(
                    onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(mapsUrl)))
                    },
                    modifier = Modifier.testTag("open_maps_photos_button")
                ) {
                    Text("Google Maps", style = MaterialTheme.typography.labelMedium)
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = "Open Google Maps photos",
                        modifier = Modifier
                            .padding(start = 4.dp)
                            .size(16.dp)
                    )
                }
            }

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(end = 4.dp)
            ) {
                items(battleBoxClubGalleryPhotos, key = { it.id }) { photo ->
                    Box(
                        modifier = Modifier
                            .width(248.dp)
                            .height(176.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { selectedPhoto = photo }
                            .testTag("showcase_photo_${photo.id}")
                    ) {
                        Image(
                            painter = painterResource(id = photo.drawableRes),
                            contentDescription = photo.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color(0x220E1114), Color(0xE8080C10))
                                    )
                                )
                        )
                        Surface(
                            color = Color(0xD80A1219),
                            border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.65f)),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(10.dp)
                        ) {
                            Text(
                                text = photo.badge,
                                style = MaterialTheme.typography.labelSmall,
                                color = CyberCyan,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = photo.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = photo.subtitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFD6DCE2),
                                maxLines = 2
                            )
                        }
                    }
                }
            }
        }
    }

    selectedPhoto?.let { item ->
        Dialog(
            onDismissRequest = { selectedPhoto = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                color = Color(0xFF0E1114),
                border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.55f)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(360.dp)
                    ) {
                        Image(
                            painter = painterResource(id = item.drawableRes),
                            contentDescription = item.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        IconButton(
                            onClick = { selectedPhoto = null },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(10.dp)
                                .background(Color(0x99000000), RoundedCornerShape(24.dp))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close photo preview",
                                tint = Color.White
                            )
                        }
                    }
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White
                        )
                        Text(
                            text = item.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFAEB8C4)
                        )
                        Button(
                            onClick = {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(mapsUrl)))
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp, bottom = 10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Open Dudhnoi Google Maps Page",
                                modifier = Modifier.padding(start = 6.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
