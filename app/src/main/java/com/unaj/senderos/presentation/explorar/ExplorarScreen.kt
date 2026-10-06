package com.unaj.senderos.presentation.explorar

import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.unaj.senderos.domain.model.Usuario

private const val TAG = "EXPLORAR"

private val ColorPrimario = Color(0xFF0D6077)
private val ColorNaranja = Color(0xFFE85D04)
private val ColorTextoPrincipal = Color(0xFF0F172A)
private val ColorTextoSecundario = Color(0xFF64748B)
private val ColorBordeSuave = Color(0xFFE2E8F0)
private val ColorFondoGrisClaro = Color(0xFFF8FAFC)

@Composable
// Coordinar el estado y los eventos de la pantalla Explorar
fun ExplorarPantalla(
    viewModel: ExplorarViewModel,
    alCerrarSesion: () -> Unit = {}
) {
    val estadoVisual by viewModel.estadoVisual.collectAsStateWithLifecycle()
    val hostEstadoSnackbar = remember { SnackbarHostState() }

    SideEffect {
        Log.d(TAG, "ExplorarPantalla renderizada / recomposición")
    }

    LaunchedEffect(Unit) {
        viewModel.efectoVisual.collect { efecto ->
            when (efecto) {
                is ExplorarEfecto.NavegarALogin -> alCerrarSesion()
                is ExplorarEfecto.MostrarMensaje -> hostEstadoSnackbar.showSnackbar(efecto.mensaje)
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = hostEstadoSnackbar) },
        containerColor = Color.White,
        topBar = {
            BarraSuperiorExplorar(usuario = estadoVisual.usuario)
        },
        bottomBar = {
            BarraNavegacionInferior(
                tabSeleccionado = estadoVisual.tabSeleccionado,
                alSeleccionarTab = { indice ->
                    viewModel.procesarEvento(ExplorarEvento.AlSeleccionarTab(indice))
                }
            )
        }
    ) { paddingValores ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValores)
        ) {
            // Contenido central (Datos confirmatorios de la sesión)
            ContenidoCentralExplorar(
                usuario = estadoVisual.usuario,
                alCerrarSesion = { viewModel.procesarEvento(ExplorarEvento.AlCerrarSesion) },
                modifier = Modifier.fillMaxSize()
            )

            // Botón flotante inferior izquierdo (+)
            BotonFlotanteInferior(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 16.dp, bottom = 16.dp)
            )
        }
    }
}

@Composable
// Renderizar la barra superior con logo, título, búsqueda, notificaciones y avatar de perfil
private fun BarraSuperiorExplorar(
    usuario: Usuario?,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Sección izquierda: Logo + ARGENTINA / Explorar
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Logo circular con icono de montaña y sol
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, Color(0xFF38BDF8), CircleShape)
                        .background(Color(0xFFE0F2FE)),
                    contentAlignment = Alignment.Center
                ) {
                    IconoMontanita()
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = "ARGENTINA",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ColorTextoSecundario,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Explorar",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorTextoPrincipal
                    )
                }
            }

            // Sección derecha: Búsqueda + Notificaciones + Avatar de Google
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = "Buscar",
                    tint = ColorTextoPrincipal,
                    modifier = Modifier.size(24.dp)
                )

                Box(contentAlignment = Alignment.TopEnd) {
                    Icon(
                        imageVector = Icons.Outlined.Notifications,
                        contentDescription = "Notificaciones",
                        tint = ColorTextoPrincipal,
                        modifier = Modifier.size(24.dp)
                    )
                    // Indicador naranja de notificación activa
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(ColorNaranja)
                    )
                }

                // Avatar de perfil (Foto obtenida del token de Google)
                AvatarUsuarioCirculo(
                    fotoUrl = usuario?.fotoUrl,
                    nombre = usuario?.nombre,
                    tamano = 36.dp
                )
            }
        }
    }
}

@Composable
// Renderizar el avatar circular del usuario cargando la imagen remota con Coil
private fun AvatarUsuarioCirculo(
    fotoUrl: String?,
    nombre: String?,
    tamano: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(tamano)
            .clip(CircleShape)
            .border(1.dp, ColorBordeSuave, CircleShape)
            .background(Color(0xFFE2E8F0)),
        contentAlignment = Alignment.Center
    ) {
        if (!fotoUrl.isNullOrBlank()) {
            AsyncImage(
                model = fotoUrl,
                contentDescription = "Foto de perfil",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
            )
        } else {
            val inicial = nombre?.firstOrNull()?.uppercase() ?: "U"
            Text(
                text = inicial,
                fontSize = (tamano.value * 0.45f).sp,
                fontWeight = FontWeight.Bold,
                color = ColorPrimario
            )
        }
    }
}

@Composable
// Renderizar el contenido central que muestra la confirmación de los datos obtenidos de Firebase
private fun ContenidoCentralExplorar(
    usuario: Usuario?,
    alCerrarSesion: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(elevation = 2.dp, shape = RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(ColorBordeSuave))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Etiqueta de estado autenticado
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFFECFDF5))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF059669),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Sesión Activa con Google",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF065F46)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Foto de perfil ampliada
                AvatarUsuarioCirculo(
                    fotoUrl = usuario?.fotoUrl,
                    nombre = usuario?.nombre,
                    tamano = 80.dp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Nombre completo obtenido del token
                Text(
                    text = usuario?.nombre?.ifBlank { "Matias Galvan" } ?: "Usuario Autenticado",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorTextoPrincipal,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Correo electrónico obtenido del token
                Text(
                    text = usuario?.email?.ifBlank { "matiasgalvan90@gmail.com" } ?: "Sin correo",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    color = ColorTextoSecundario,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                HorizontalDivider(color = Color(0xFFF1F5F9))

                Spacer(modifier = Modifier.height(16.dp))

                // Detalle técnico del UID obtenido de Firebase
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(ColorFondoGrisClaro)
                        .padding(12.dp)
                ) {
                    Text(
                        text = "UID de Firebase Auth:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ColorTextoSecundario
                    )
                    Text(
                        text = usuario?.id ?: "Pendiente de token",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = ColorPrimario
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Botón para cerrar sesión
                OutlinedButton(
                    onClick = alCerrarSesion,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFDC2626)
                    ),
                    border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                        brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFFCA5A5))
                    )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ExitToApp,
                        contentDescription = "Cerrar sesión",
                        tint = Color(0xFFDC2626),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Cerrar sesión",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
// Renderizar el botón flotante circular ubicado en la esquina inferior izquierda
private fun BotonFlotanteInferior(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(46.dp)
            .clip(CircleShape)
            .border(1.5.dp, Color(0xFF0284C7), CircleShape)
            .background(Color.White)
            .clickable { /* Acción flotante futura */ },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Add,
            contentDescription = "Agregar",
            tint = Color(0xFF0284C7),
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
// Renderizar la barra de navegación inferior con los 5 accesos del diseño
private fun BarraNavegacionInferior(
    tabSeleccionado: Int,
    alSeleccionarTab: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(vertical = 8.dp, horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            // Item 1: Explorar
            ItemBarraNavegacion(
                icono = Icons.Outlined.Explore,
                etiqueta = "Explorar",
                estaSeleccionado = tabSeleccionado == 0,
                alHacerClic = { alSeleccionarTab(0) }
            )

            // Item 2: Rutas
            ItemBarraNavegacion(
                icono = Icons.Outlined.Add,
                etiqueta = "Rutas",
                estaSeleccionado = tabSeleccionado == 1,
                alHacerClic = { alSeleccionarTab(1) }
            )

            // Item 3 (Centro): Botón naranja con letra A
            BotonCentralMarca(alHacerClic = { alSeleccionarTab(2) })

            // Item 4: Guardado
            ItemBarraNavegacion(
                icono = Icons.Outlined.BookmarkBorder,
                etiqueta = "Guardado",
                estaSeleccionado = tabSeleccionado == 3,
                alHacerClic = { alSeleccionarTab(3) }
            )

            // Item 5: Perfil
            ItemBarraNavegacion(
                icono = Icons.Outlined.PersonOutline,
                etiqueta = "Perfil",
                estaSeleccionado = tabSeleccionado == 4,
                alHacerClic = { alSeleccionarTab(4) }
            )
        }
    }
}

@Composable
// Renderizar un elemento individual de la barra inferior
private fun ItemBarraNavegacion(
    icono: androidx.compose.ui.graphics.vector.ImageVector,
    etiqueta: String,
    estaSeleccionado: Boolean,
    alHacerClic: () -> Unit
) {
    val colorElemento = if (estaSeleccionado) ColorPrimario else ColorTextoSecundario

    Column(
        modifier = Modifier
            .clickable(onClick = alHacerClic)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icono,
            contentDescription = etiqueta,
            tint = colorElemento,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = etiqueta,
            fontSize = 11.sp,
            fontWeight = if (estaSeleccionado) FontWeight.SemiBold else FontWeight.Normal,
            color = colorElemento
        )
    }
}

@Composable
// Renderizar el botón central de marca con la letra A sobre fondo naranja redondeado
private fun BotonCentralMarca(
    alHacerClic: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(width = 46.dp, height = 34.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(ColorNaranja)
            .clickable(onClick = alHacerClic),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "A",
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White
        )
    }
}

@Composable
// Dibujar el icono gráfico representativo de una montaña y sol
private fun IconoMontanita() {
    Canvas(modifier = Modifier.size(22.dp)) {
        val ancho = size.width
        val alto = size.height

        // Montaña azul
        val caminoMontana = Path().apply {
            moveTo(ancho * 0.1f, alto * 0.85f)
            lineTo(ancho * 0.5f, alto * 0.2f)
            lineTo(ancho * 0.9f, alto * 0.85f)
            close()
        }
        drawPath(path = caminoMontana, color = ColorPrimario)

        // Sol amarillo en la cima
        drawCircle(
            color = Color(0xFFF59E0B),
            radius = ancho * 0.18f,
            center = Offset(ancho * 0.5f, alto * 0.38f)
        )
    }
}
