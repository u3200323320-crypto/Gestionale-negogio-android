package it.casagestionale.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Ink = Color(0xFF252B27)
private val Green = Color(0xFF365A4D)
private val MutedGreen = Color(0xFFE9F0EB)
private val Canvas = Color(0xFFF5F6F2)
private val Line = Color(0xFFE2E6E0)
private val Muted = Color(0xFF747C76)
private val Alert = Color(0xFF9B5A39)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val database = StoreDatabase(applicationContext)
        window.statusBarColor = android.graphics.Color.rgb(245, 246, 242)
        window.navigationBarColor = android.graphics.Color.rgb(245, 246, 242)
        window.decorView.systemUiVisibility =
            android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = Green,
                    onPrimary = Color.White,
                    secondary = MutedGreen,
                    background = Canvas,
                    surface = Color.White,
                    onSurface = Ink,
                    onBackground = Ink,
                    outline = Line,
                    error = Alert,
                ),
            ) {
                StoreApp(database)
            }
        }
    }
}

private enum class Screen(val title: String) {
    Home("Panoramica"), Inventory("Magazzino"), Checkout("Cassa"), Sales("Vendite"), Statistics("Statistiche"),
}

private data class CartLine(val product: Product, val quantity: Int)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StoreApp(database: StoreDatabase) {
    val appContext = LocalContext.current.applicationContext
    var screen by remember { mutableStateOf(Screen.Home) }
    var revision by remember { mutableStateOf(0) }
    val stats = remember(revision) { database.stats() }
    val products = remember(revision) { database.products() }

    Scaffold(
        containerColor = Canvas,
        contentWindowInsets = WindowInsets.navigationBars,
        topBar = {
            TopAppBar(
                title = {
                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text("CASA GESTIONALE", style = MaterialTheme.typography.labelSmall, color = Muted, fontWeight = FontWeight.SemiBold)
                        Text(screen.title, style = MaterialTheme.typography.titleLarge, color = Ink, fontWeight = FontWeight.SemiBold)
                    }
                },
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(containerColor = Canvas),
            )
        },
        bottomBar = {
            NavigationBar(containerColor = Color.White, tonalElevation = 0.dp) {
                Screen.entries.forEach { item ->
                    val icon = when (item) {
                        Screen.Home -> Icons.Filled.Home
                        Screen.Inventory -> Icons.Filled.Inventory2
                        Screen.Checkout -> Icons.Filled.PointOfSale
                        Screen.Sales -> Icons.Filled.ReceiptLong
                        Screen.Statistics -> Icons.Filled.BarChart
                    }
                    NavigationBarItem(
                        selected = screen == item,
                        onClick = { screen = item },
                        icon = { Icon(icon, contentDescription = item.title) },
                        label = { Text(item.title, maxLines = 1, style = MaterialTheme.typography.labelSmall) },
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (screen) {
                Screen.Home -> HomeScreen(stats, products, onScreen = { screen = it })
                Screen.Inventory -> InventoryScreen(database, products, onChange = { revision++ })
                Screen.Checkout -> CheckoutScreen(database, products, appContext, onSaleComplete = { revision++ })
                Screen.Sales -> SalesScreen(database, revision)
                Screen.Statistics -> StatisticsScreen(stats)
            }
        }
    }
}

@Composable
private fun HomeScreen(stats: DashboardStats, products: List<Product>, onScreen: (Screen) -> Unit) {
    val lowStock = products.filter { it.stock <= it.minStock }.take(4)
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            Text(
                SimpleDateFormat("EEEE d MMMM", Locale.ITALIAN).format(Date()).replaceFirstChar { it.uppercase() },
                color = Muted,
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricTile("Incasso di oggi", euro(stats.todayTotal), "${stats.todayCount} scontrini", Modifier.weight(1f), prominent = true)
                MetricTile("Articoli", stats.productCount.toString(), "${stats.lowStockCount} scorte basse", Modifier.weight(1f))
            }
        }
        item {
            SectionHeader("Accesso rapido")
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickAction("Nuova vendita", Icons.Filled.PointOfSale, Modifier.weight(1f)) { onScreen(Screen.Checkout) }
                QuickAction("Aggiungi articolo", Icons.Filled.Add, Modifier.weight(1f)) { onScreen(Screen.Inventory) }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                SectionHeader("Da riordinare")
                TextButton(onClick = { onScreen(Screen.Inventory) }) { Text("Vedi magazzino") }
            }
        }
        if (lowStock.isEmpty()) {
            item { EmptyState("Nessuna scorta da riordinare", "Le disponibilità sono in regola.") }
        } else {
            items(lowStock, key = { it.id }) { product -> ProductRow(product, onClick = { onScreen(Screen.Inventory) }) }
        }
        item {
            SectionHeader("Riepilogo settimanale")
            Spacer(Modifier.height(10.dp))
            MetricTile("Incasso ultimi 7 giorni", euro(stats.weekTotal), "Valore merce a costo: ${euro(stats.stockValue)}", Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun InventoryScreen(database: StoreDatabase, products: List<Product>, onChange: () -> Unit) {
    var query by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<Product?>(null) }
    var creating by remember { mutableStateOf(false) }
    val visible = remember(query, products) {
        if (query.isBlank()) products else products.filter {
            it.name.contains(query, true) || it.category.contains(query, true) || it.barcode.contains(query, true)
        }
    }
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                placeholder = { Text("Cerca nome o codice") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
            )
            if (visible.isEmpty()) {
                EmptyState("Nessun articolo trovato", "Aggiungi un prodotto per iniziare a gestire il magazzino.")
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item { Text("${visible.size} articoli", color = Muted, style = MaterialTheme.typography.labelMedium) }
                    items(visible, key = { it.id }) { product -> ProductRow(product) { editing = product } }
                }
            }
        }
        FloatingActionButton(
            onClick = { creating = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
            containerColor = Green,
            contentColor = Color.White,
            shape = RoundedCornerShape(14.dp),
        ) { Icon(Icons.Filled.Add, contentDescription = "Aggiungi articolo") }
    }
    if (creating || editing != null) {
        ProductEditor(
            product = editing,
            onDismiss = { creating = false; editing = null },
            onSave = { product ->
                database.saveProduct(product)
                creating = false
                editing = null
                onChange()
            },
        )
    }
}

@Composable
private fun ProductEditor(product: Product?, onDismiss: () -> Unit, onSave: (Product) -> Unit) {
    var name by remember(product) { mutableStateOf(product?.name.orEmpty()) }
    var category by remember(product) { mutableStateOf(product?.category.orEmpty()) }
    var barcode by remember(product) { mutableStateOf(product?.barcode.orEmpty()) }
    var stock by remember(product) { mutableStateOf(product?.stock?.toString() ?: "0") }
    var minimum by remember(product) { mutableStateOf(product?.minStock?.toString() ?: "3") }
    var cost by remember(product) { mutableStateOf(product?.cost?.toString() ?: "0") }
    var price by remember(product) { mutableStateOf(product?.price?.toString() ?: "0") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (product == null) "Nuovo articolo" else "Modifica articolo") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.heightIn(max = 480.dp)) {
                item { FormField("Nome articolo", name, { name = it }) }
                item { FormField("Categoria", category, { category = it }) }
                item { FormField("Codice a barre", barcode, { barcode = it }) }
                item { FormField("Quantità disponibile", stock, { stock = it }, numeric = true) }
                item { FormField("Avviso scorta sotto", minimum, { minimum = it }, numeric = true) }
                item { FormField("Costo unitario", cost, { cost = it }, numeric = true) }
                item { FormField("Prezzo di vendita", price, { price = it }, numeric = true) }
            }
        },
        confirmButton = {
            Button(onClick = {
                val cleanName = name.trim()
                if (cleanName.isNotEmpty()) onSave(
                    Product(
                        product?.id ?: 0,
                        cleanName,
                        category,
                        barcode,
                        stock.toIntOrNull() ?: 0,
                        minimum.toIntOrNull() ?: 0,
                        cost.replace(',', '.').toDoubleOrNull() ?: 0.0,
                        price.replace(',', '.').toDoubleOrNull() ?: 0.0,
                    ),
                )
            }, enabled = name.isNotBlank()) { Text("Salva") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla") } },
    )
}

@Composable
private fun FormField(label: String, value: String, onValueChange: (String) -> Unit, numeric: Boolean = false) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = if (numeric) KeyboardType.Decimal else KeyboardType.Text),
        shape = RoundedCornerShape(8.dp),
    )
}

@Composable
private fun CheckoutScreen(database: StoreDatabase, products: List<Product>, appContext: Context, onSaleComplete: () -> Unit) {
    var query by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var payment by remember { mutableStateOf("Contanti") }
    var cameraOpen by remember { mutableStateOf(false) }
    var recognizeProduct by remember { mutableStateOf(false) }
    var showPermissionMessage by remember { mutableStateOf(false) }
    val cart = remember { mutableStateMapOf<Long, CartLine>() }
    val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        cameraOpen = granted
        if (!granted) showPermissionMessage = true
    }
    val matching = remember(query, products) {
        if (query.isBlank()) products.take(8) else products.filter {
            it.name.contains(query, true) || it.category.contains(query, true) || it.barcode.contains(query, true)
        }.take(8)
    }
    val total = cart.values.sumOf { it.product.price * it.quantity }

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Cerca o inserisci codice") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
            )
            OutlinedButton(
                onClick = {
                    if (ContextCompat.checkSelfPermission(appContext, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                        cameraOpen = true
                    } else cameraPermission.launch(Manifest.permission.CAMERA)
                },
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 16.dp),
            ) { Icon(Icons.Filled.CameraAlt, contentDescription = "Scansiona prodotto") }
        }
        message?.let { Text(it, color = Green, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp)) }
        Text("Articoli", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 18.dp, bottom = 7.dp))
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            if (matching.isEmpty()) item { EmptyState("Nessun prodotto", "Prova un altro nome oppure aggiungilo dal magazzino.") }
            items(matching, key = { it.id }) { product ->
                ProductRow(product, trailing = {
                    TextButton(onClick = {
                        val current = cart[product.id]?.quantity ?: 0
                        if (current < product.stock) cart[product.id] = CartLine(product, current + 1)
                        else message = "Disponibilità esaurita per ${product.name}"
                    }, enabled = product.stock > 0) { Icon(Icons.Filled.Add, contentDescription = "Aggiungi al conto") }
                })
            }
        }
        if (cart.isNotEmpty()) {
            Divider(color = Line)
            Text("Conto · ${cart.values.sumOf { it.quantity }} pezzi", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
            cart.values.toList().forEach { line ->
                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("${line.quantity} × ${line.product.name}", modifier = Modifier.weight(1f), maxLines = 1)
                    Text(euro(line.quantity * line.product.price), fontWeight = FontWeight.Medium)
                    IconButton(onClick = {
                        if (line.quantity == 1) cart.remove(line.product.id)
                        else cart[line.product.id] = line.copy(quantity = line.quantity - 1)
                    }, modifier = Modifier.size(34.dp)) { Text("−", color = Muted) }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp, bottom = 4.dp)) {
                listOf("Contanti", "Carta").forEach { method ->
                    FilterChip(selected = payment == method, onClick = { payment = method }, label = { Text(method) })
                }
            }
            Row(Modifier.fillMaxWidth().padding(bottom = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Totale", color = Muted, style = MaterialTheme.typography.bodySmall)
                    Text(euro(total), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = {
                        try {
                            database.completeSale(cart.values.associate { it.product to it.quantity }, payment)
                            cart.clear()
                            query = ""
                            message = "Vendita registrata"
                            onSaleComplete()
                        } catch (error: IllegalStateException) {
                            message = error.message
                        }
                    },
                    enabled = cart.isNotEmpty(),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
                ) { Icon(Icons.Filled.Check, contentDescription = null); Spacer(Modifier.width(7.dp)); Text("Concludi") }
            }
        }
    }

    if (cameraOpen) {
        AlertDialog(
            onDismissRequest = { cameraOpen = false },
            title = { Text("Aggiungi da fotocamera") },
            text = {
                Column {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = !recognizeProduct, onClick = { recognizeProduct = false }, label = { Text("Barcode") })
                        FilterChip(selected = recognizeProduct, onClick = { recognizeProduct = true }, label = { Text("Prodotto") })
                    }
                    Spacer(Modifier.height(8.dp))
                    CameraScanner(
                        modifier = Modifier.fillMaxWidth().height(280.dp),
                        recognizeProduct = recognizeProduct,
                        onBarcode = { code ->
                            val product = database.productByBarcode(code)
                            if (product == null) {
                                query = code
                                message = "Codice $code non presente in magazzino"
                            } else if ((cart[product.id]?.quantity ?: 0) < product.stock) {
                                val quantity = (cart[product.id]?.quantity ?: 0) + 1
                                cart[product.id] = CartLine(product, quantity)
                                message = "${product.name} aggiunto alla vendita"
                            } else message = "Disponibilità esaurita per ${product.name}"
                            cameraOpen = false
                        },
                        onLabel = { label ->
                            val candidate = products.firstOrNull {
                                it.name.contains(label, true) || it.category.contains(label, true)
                            }
                            if (candidate != null) {
                                val quantity = (cart[candidate.id]?.quantity ?: 0) + 1
                                if (quantity <= candidate.stock) {
                                    cart[candidate.id] = CartLine(candidate, quantity)
                                    message = "Riconosciuto: ${candidate.name}"
                                } else message = "Disponibilità esaurita per ${candidate.name}"
                            } else {
                                query = label
                                message = "Riconosciuto: $label. Verifica il catalogo."
                            }
                            cameraOpen = false
                        },
                    )
                }
            },
            confirmButton = { TextButton(onClick = { cameraOpen = false }) { Text("Chiudi") } },
        )
    }
    if (showPermissionMessage) {
        AlertDialog(
            onDismissRequest = { showPermissionMessage = false },
            title = { Text("Fotocamera non disponibile") },
            text = { Text("Abilita il permesso fotocamera nelle impostazioni dell'app.") },
            confirmButton = { TextButton(onClick = { showPermissionMessage = false }) { Text("Ho capito") } },
        )
    }
}

@Composable
private fun SalesScreen(database: StoreDatabase, revision: Int) {
    val sales = remember(revision) { database.sales() }
    if (sales.isEmpty()) {
        EmptyState("Nessuna vendita registrata", "Le vendite completate in cassa compariranno qui.")
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item { Text("${sales.size} scontrini", color = Muted, style = MaterialTheme.typography.labelMedium) }
            items(sales, key = { it.id }) { sale ->
                Card(shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Scontrino #${sale.id}", fontWeight = FontWeight.SemiBold)
                            Text("${formatDate(sale.date)} · ${sale.items} pezzi · ${sale.payment}", color = Muted, style = MaterialTheme.typography.bodySmall)
                        }
                        Text(euro(sale.total), fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatisticsScreen(stats: DashboardStats) {
    val difference = stats.weekTotal - stats.previousWeekTotal
    val largest = maxOf(stats.weekTotal, stats.previousWeekTotal)
    val currentBar = if (largest == 0.0) 0f else (stats.weekTotal / largest).toFloat()
    val previousBar = if (largest == 0.0) 0f else (stats.previousWeekTotal / largest).toFloat()
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            SectionHeader("Andamento incassi")
            Spacer(Modifier.height(14.dp))
            ChartBar("Questa settimana", stats.weekTotal, currentBar, Green)
            Spacer(Modifier.height(14.dp))
            ChartBar("Settimana precedente", stats.previousWeekTotal, previousBar, Color(0xFFB8C7BE))
            Text(
                if (difference >= 0) "${euro(difference)} in più rispetto alla settimana scorsa"
                else "${euro(-difference)} in meno rispetto alla settimana scorsa",
                color = Muted,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
        item { MetricTile("Incasso oggi", euro(stats.todayTotal), "${stats.todayCount} scontrini", Modifier.fillMaxWidth()) }
        item { MetricTile("Valore del magazzino", euro(stats.stockValue), "Valutato al costo di acquisto", Modifier.fillMaxWidth()) }
        item {
            Card(shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = if (stats.lowStockCount > 0) Color(0xFFF4EEE8) else MutedGreen)) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Filled.WarningAmber, contentDescription = null, tint = if (stats.lowStockCount > 0) Alert else Green)
                    Column {
                        Text("${stats.lowStockCount} articoli da riordinare", fontWeight = FontWeight.SemiBold)
                        Text("${stats.productCount} articoli totali in catalogo", color = Muted, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun ChartBar(label: String, amount: Double, fraction: Float, color: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = Muted, style = MaterialTheme.typography.bodySmall)
            Text(euro(amount), fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
        }
        Box(Modifier.fillMaxWidth().height(9.dp).background(Line, RoundedCornerShape(5.dp))) {
            if (fraction > 0f) {
                Box(Modifier.fillMaxWidth(fraction.coerceIn(0.02f, 1f)).height(9.dp).background(color, RoundedCornerShape(5.dp)))
            }
        }
    }
}

@Composable
private fun ProductRow(product: Product, onClick: () -> Unit = {}, trailing: (@Composable () -> Unit)? = null) {
    val low = product.stock <= product.minStock
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Row(Modifier.fillMaxWidth().padding(start = 14.dp, top = 11.dp, bottom = 11.dp, end = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(product.name, fontWeight = FontWeight.Medium, maxLines = 1)
                Text(
                    listOfNotNull(product.category.takeIf { it.isNotBlank() }, "${product.stock} disponibili").joinToString(" · "),
                    color = if (low) Alert else Muted,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(euro(product.price), fontWeight = FontWeight.SemiBold)
                if (low) Text("Scorta bassa", color = Alert, style = MaterialTheme.typography.labelSmall)
            }
            trailing?.invoke()
        }
    }
}

@Composable
private fun MetricTile(title: String, value: String, detail: String, modifier: Modifier = Modifier, prominent: Boolean = false) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = if (prominent) MutedGreen else Color.White),
    ) {
        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, color = Muted, style = MaterialTheme.typography.labelMedium)
            Text(value, color = Ink, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Text(detail, color = Muted, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun QuickAction(label: String, icon: ImageVector, modifier: Modifier, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 54.dp),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, maxLines = 1, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun EmptyState(title: String, detail: String) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 34.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Text(title, fontWeight = FontWeight.SemiBold)
        Text(detail, color = Muted, style = MaterialTheme.typography.bodySmall)
    }
}

private fun euro(amount: Double): String = String.format(Locale.ITALY, "€ %.2f", amount)

private fun formatDate(timestamp: Long): String = SimpleDateFormat("d MMM · HH:mm", Locale.ITALY).format(Date(timestamp))
