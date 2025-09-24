@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.encasaxo.hq

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

// CameraX & MLKit imports
import android.Manifest
import android.content.pm.PackageManager
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview as CameraPreview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import androidx.compose.material3.AlertDialog
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.encasaxo.hq.network.dto.UpdatePackingListBody
import com.encasaxo.hq.network.dto.UpdateItem
import com.encasaxo.hq.network.dto.BoxItemUpdate
import com.encasaxo.hq.network.dto.CreatePackingListBody
import com.encasaxo.hq.network.dto.PackingForm
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateMapOf

// Navigation imports
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

// ViewModel import
import com.encasaxo.hq.data.PackingListViewModel
import com.encasaxo.hq.data.PackingListUiState
import com.encasaxo.hq.network.dto.PackingListHeader

class MainActivity : ComponentActivity() {
    // Activity-scoped ViewModel (default factory)
    private val packingListViewModel: PackingListViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EncasaHQApp(viewModel = packingListViewModel)
        }
    }
}

@Composable
fun EncasaHQApp(viewModel: PackingListViewModel) {
    MaterialTheme {
        Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
            val navController = rememberNavController()
            Column(modifier = Modifier.fillMaxSize()) {
                // Main content
                Box(modifier = Modifier.weight(1f)) {
            AppNavHost(navController = navController, viewModel = viewModel)
                }
                // Bottom navigation
                NavigationBar {
                    val currentRoute = navController.currentBackStackEntry?.destination?.route ?: ""
                    NavigationBarItem(
                        selected = currentRoute.startsWith("packing_list") || currentRoute.startsWith("packing_view") || currentRoute.startsWith("packing_create"),
                        onClick = { navController.navigate("packing_list") },
                        icon = { Icon(imageVector = Icons.Default.ArrowBack, contentDescription = null) },
                        label = { Text("Lists") },
                        colors = NavigationBarItemDefaults.colors()
                    )
                    NavigationBarItem(
                        selected = currentRoute.startsWith("mapping"),
                        onClick = { navController.navigate("mapping") },
                        icon = { Icon(imageVector = Icons.Default.Add, contentDescription = null) },
                        label = { Text("Mapping") },
                        colors = NavigationBarItemDefaults.colors()
                    )
                }
            }
        }
    }
}

@Composable
fun AppNavHost(navController: NavHostController, viewModel: PackingListViewModel) {
    NavHost(navController = navController, startDestination = "packing_list") {
        composable("packing_list") {
            PackingListScreenWithViewModel(
                viewModel = viewModel,
                onOpen = { id -> navController.navigate("packing_view/$id") },
                onCreate = { navController.navigate("packing_create") }
            )
        }

        composable("packing_view/{id}") { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id").orEmpty()
            PackingViewScreen(id = id, onBack = { navController.popBackStack() }, onScan = { navController.navigate("scan_mode/$id") })
        }

        composable("packing_create") {
            // Keep simple: when done, call viewModel.create(...) (if implemented) or refresh
            PackingCreateScreen(onDone = { newId ->
                // If you implement viewModel.create, call it and then refresh. For now we refresh list to reflect changes.
                viewModel.refresh()
                navController.popBackStack()
            }, onCancel = { navController.popBackStack() })
        }

        composable("scan_mode/{id}") { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id").orEmpty()
            ScanModeScreen(packingListId = id, onBack = { navController.popBackStack() })
        }

        composable("mapping") {
            // Placeholder mapping screen for bottom nav (simple instructions for now)
            Scaffold(topBar = { TopAppBar(title = { Text("Barcode Mapping") }) }) { innerPadding ->
                Column(modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)) {
                    Text(text = "Use Scan Mode to scan and map barcodes. Refresh cache from Scan screen.")
                }
            }
        }
    }
}

/**
 * UI layer that reads state from the ViewModel.
 */
@Composable
fun PackingListScreenWithViewModel(
    viewModel: PackingListViewModel,
    onOpen: (String) -> Unit,
    onCreate: () -> Unit
) {
    // Collect uiState
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Packing Lists") },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Refresh")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreate) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Create")
            }
        }
    ) { innerPadding ->
        when (uiState) {
            is PackingListUiState.Loading -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                    Text(text = "Loading...", modifier = Modifier.padding(16.dp))
                }
            }

            is PackingListUiState.Error -> {
                val msg = (uiState as PackingListUiState.Error).message
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Top
                ) {
                    Text(text = "Error: $msg")
                    Button(onClick = { viewModel.refresh() }, modifier = Modifier.padding(top = 12.dp)) {
                        Text("Retry")
                    }
                }
            }

            is PackingListUiState.Success -> {
                val items = (uiState as PackingListUiState.Success).items
                if (items.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "No packing lists yet. Tap + to create one.",
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(8.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        items(items) { header ->
                            PackingListRow(header = header, onClick = { onOpen(header.id) })
                        }
                    }
                }
            }
        }
    }
}

/* Reusable UI components */
@Composable
fun PackingListRow(header: PackingListHeader, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp)
    ) {
        val title = header.shipmentId?.takeIf { it.isNotBlank() } ?: (header.production ?: "")
        Text(text = title, fontWeight = FontWeight.Bold)
        Row(modifier = Modifier.fillMaxWidth().padding(top = 2.dp)) {
            if (!header.production.isNullOrBlank()) {
                Text(text = "Production: ${header.production}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
            }
            if (!header.status.isNullOrBlank()) {
                Box(modifier = Modifier) {
                    Text(
                        text = header.status,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier
                            .padding(start = 8.dp)
                    )
                }
            }
        }
        if (!header.dispatchDate.isNullOrBlank()) {
            Text(text = "Dispatch: ${header.dispatchDate}", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun PackingViewScreen(id: String, onBack: () -> Unit, onScan: () -> Unit) {
    // Resolve shipment_id for display
    var title by rememberSaveable { mutableStateOf(id) }
    var detailsExpanded by rememberSaveable { mutableStateOf(false) }
    val repository = remember { com.encasaxo.hq.data.PackingListRepository() }
    var viewState by rememberSaveable { mutableStateOf<com.encasaxo.hq.network.dto.PackingListViewResponse?>(null) }
    LaunchedEffect(id) {
        try {
            val resp = repository.view(id)
            if (resp.isSuccessful) {
                val body = resp.body()
                viewState = body
                val shipment = body?.packingList?.shipmentId
                if (!shipment.isNullOrBlank()) title = shipment
            }
        } catch (_: Exception) { }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("View Packing") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(16.dp)
        ) {
            Text(text = "Packing List:", fontWeight = FontWeight.Bold)
            Text(text = title, modifier = Modifier.padding(top = 8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp)
            ) {
                Button(
                    onClick = { detailsExpanded = !detailsExpanded },
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 6.dp)
                ) {
                    Text("View Details")
                }
                Button(
                    onClick = onScan,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 6.dp)
                ) {
                    Text("Scan Mode")
                }
            }

            if (detailsExpanded) {
                val items = viewState?.items.orEmpty()
                Text(text = "Items", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp))

                // Header row (Box, SKU, Design, Qty)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 4.dp)
                ) {
                    Text("Box", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(0.5f))
                    Text("SKU", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(2.2f))
                    Text("Design", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(2.2f))
                    Text("Qty", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(0.5f))
                }

                // Flatten rows: one row per box detail
                val rows = buildList {
                    items.forEach { itItem ->
                        val sku = itItem.sku ?: ""
                        val design = itItem.design ?: ""
                        itItem.boxDetails.sortedBy { b -> (b.boxNo ?: 0) }.forEach { b ->
                            add(
                                arrayOf(
                                    (b.boxNo ?: 0).toString(),
                                    sku,
                                    design,
                                    (b.quantity ?: 0).toString()
                                )
                            )
                        }
                    }
                }

                LazyColumn(
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                ) {
                    items(rows) { r ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Text(r[0], modifier = Modifier.weight(0.5f))
                            Text(r[1], modifier = Modifier.weight(2.2f))
                            Text(r[2], modifier = Modifier.weight(2.2f))
                            Text(r[3], modifier = Modifier.weight(0.5f), textAlign = androidx.compose.ui.text.style.TextAlign.End)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ScanModeScreen(packingListId: String, onBack: () -> Unit) {
    var code by rememberSaveable { mutableStateOf("") }
    var result by rememberSaveable { mutableStateOf("") }
    var qtyText by rememberSaveable { mutableStateOf("") }
    var boxNoText by rememberSaveable { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val repository = remember { com.encasaxo.hq.data.PackingListRepository() }
    var showScanner by rememberSaveable { mutableStateOf(true) }
    var title by rememberSaveable { mutableStateOf(packingListId) }
    var lastSku by rememberSaveable { mutableStateOf("") }
    var lastProduct by rememberSaveable { mutableStateOf("") }
    var lastLine by rememberSaveable { mutableStateOf("") }
    var lastDesign by rememberSaveable { mutableStateOf("") }
    var lastSize by rememberSaveable { mutableStateOf("") }
    var lastPackOf by rememberSaveable { mutableStateOf(0) }
    val stagedBoxes = remember { mutableStateMapOf<Int, Int>() } // boxNo -> qty to add/update

    // Fetch shipment_id to display instead of internal id
    LaunchedEffect(packingListId) {
        try {
            val resp = repository.view(packingListId)
            if (resp.isSuccessful) {
                val body = resp.body()
                val header = body?.packingList
                val shipment = header?.shipmentId
                if (!shipment.isNullOrBlank()) {
                    title = shipment
                }
            }
        } catch (_: Exception) { }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scan Mode") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Text(text = "Packing List: $title", fontWeight = FontWeight.Bold)

            if (showScanner) {
                BarcodeScannerView(
                    onBarcode = { value ->
                        code = value
                        showScanner = false
                        scope.launch {
                            try {
                                val resp = repository.barcodeMapping(code.trim())
                                result = if (resp.isSuccessful) {
                                    val body = resp.body()
                                    when {
                                        body == null -> "Not found"
                                        body.fnsku != null -> {
                                            lastSku = body.sku ?: ""
                                            lastProduct = body.product ?: ""
                                            lastLine = body.line ?: ""
                                            lastDesign = body.color ?: ""
                                            lastSize = body.size ?: ""
                                            lastPackOf = body.pcsPack ?: 0
                                            "SKU: ${body.sku}\nProduct: ${body.product}\nLine: ${body.line}\nColor: ${body.color}\nSize: ${body.size}\nPack: ${body.pcsPack}"
                                        }
                                        else -> "Unexpected response"
                                    }
                                } else {
                                    val err = resp.errorBody()?.string()
                                    "Error: ${resp.code()} ${resp.message()}${if (!err.isNullOrBlank()) "\n$err" else ""}"
                                }
                            } catch (e: Exception) {
                                result = "Error: ${e.message}"
                            }
                        }
                    }
                )
            }

            OutlinedTextField(
                value = code,
                onValueChange = { code = it },
                label = { Text("Enter/Scan Barcode") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            )
            Button(
                onClick = {
                    scope.launch {
                        try {
                            val resp = repository.barcodeMapping(code.trim())
                            result = if (resp.isSuccessful) {
                                val body = resp.body()
                                when {
                                    body == null -> "Not found"
                                    body.fnsku != null -> {
                                        lastSku = body.sku ?: ""
                                        lastProduct = body.product ?: ""
                                        lastLine = body.line ?: ""
                                        lastDesign = body.color ?: ""
                                        lastSize = body.size ?: ""
                                        lastPackOf = body.pcsPack ?: 0
                                        "SKU: ${body.sku}\nProduct: ${body.product}\nLine: ${body.line}\nColor: ${body.color}\nSize: ${body.size}\nPack: ${body.pcsPack}"
                                    }
                                    else -> "Unexpected response"
                                }
                            } else {
                                val err = resp.errorBody()?.string()
                                "Error: ${resp.code()} ${resp.message()}${if (!err.isNullOrBlank()) "\n$err" else ""}"
                            }
                        } catch (e: Exception) {
                            result = "Error: ${e.message}"
                        }
                    }
                },
                modifier = Modifier
                    .padding(top = 12.dp)
            ) {
                Text("Lookup")
            }
            if (result.isNotBlank()) {
                Text(text = result, modifier = Modifier.padding(top = 16.dp))
            }

            // Quantity and Box Number inputs
            OutlinedTextField(
                value = qtyText,
                onValueChange = { qtyText = it.filter { ch -> ch.isDigit() } },
                label = { Text("Quantity") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            )

            OutlinedTextField(
                value = boxNoText,
                onValueChange = { boxNoText = it.filter { ch -> ch.isDigit() } },
                label = { Text("Box Number") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )

            Row(modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)) {
                Button(
                    onClick = {
                        val qty = qtyText.toIntOrNull() ?: 0
                        val boxNo = boxNoText.toIntOrNull() ?: 0
                        if (lastSku.isBlank()) {
                            result = "Scan or lookup a barcode first."
                            return@Button
                        }
                        if (qty <= 0 || boxNo <= 0) {
                            result = "Enter valid Quantity and Box Number."
                            return@Button
                        }
                        stagedBoxes[boxNo] = qty
                        qtyText = ""
                        boxNoText = ""
                        result = buildString {
                            append("Staged: ")
                            append(stagedBoxes.entries.sortedBy { it.key }.joinToString { "Box ${it.key}=${it.value}" })
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 6.dp)
                ) {
                    Text("Add to Box")
                }
                Button(
                    onClick = {
                        scope.launch {
                            if (lastSku.isBlank()) {
                                result = "Scan or lookup a barcode first."
                                return@launch
                            }
                            if (stagedBoxes.isEmpty()) {
                                result = "Nothing to save. Use Add to Box first."
                                return@launch
                            }
                            try {
                                val viewResp = repository.view(packingListId)
                                if (!viewResp.isSuccessful) {
                                    result = "Failed to load items: ${viewResp.code()}"
                                    return@launch
                                }
                                val body = viewResp.body()
                                val items = body?.items.orEmpty()
                                val existing = items.firstOrNull { (it.sku ?: "").equals(lastSku, ignoreCase = true) }

                                val merged = mutableMapOf<Int, Int>()
                                if (existing != null) {
                                    existing.boxDetails.forEach { bi ->
                                        val no = bi.boxNo ?: return@forEach
                                        val q = bi.quantity ?: 0
                                        merged[no] = q
                                    }
                                }
                                // Apply staged
                                stagedBoxes.forEach { (no, q) -> merged[no] = q }

                                val updates = merged.entries.map { BoxItemUpdate(boxNo = it.key, quantity = it.value) }
                                val total = merged.values.sum()

                                val updateItem = if (existing == null) {
                                    UpdateItem(
                                        id = null,
                                        sku = lastSku,
                                        product = lastProduct.ifBlank { null },
                                        line = lastLine.ifBlank { null },
                                        design = lastDesign.ifBlank { null },
                                        size = lastSize.ifBlank { null },
                                        packOf = if (lastPackOf <= 0) null else lastPackOf,
                                        totalQty = total,
                                        boxDetails = updates,
                                        status = null
                                    )
                                } else {
                                    UpdateItem(
                                        id = existing.id,
                                        sku = existing.sku ?: lastSku,
                                        product = existing.product ?: lastProduct.ifBlank { null },
                                        line = existing.line ?: lastLine.ifBlank { null },
                                        design = existing.design ?: lastDesign.ifBlank { null },
                                        size = existing.size ?: lastSize.ifBlank { null },
                                        packOf = existing.packOf ?: (if (lastPackOf <= 0) null else lastPackOf),
                                        totalQty = total,
                                        boxDetails = updates,
                                        status = existing.status
                                    )
                                }

                                val updBody = UpdatePackingListBody(items = listOf(updateItem), cartonDetails = emptyList())
                                val updResp = repository.update(packingListId, updBody)
                                if (updResp.isSuccessful) {
                                    result = "Saved ${stagedBoxes.size} box(es). Total $total"
                                    stagedBoxes.clear()
                                    // Reactivate scan mode for the next item
                                    showScanner = true
                                    code = ""
                                    qtyText = ""
                                    boxNoText = ""
                                } else {
                                    result = "Save failed: ${updResp.code()}"
                                }
                            } catch (e: Exception) {
                                result = "Save error: ${e.message}"
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 6.dp)
                ) {
                    Text("Save")
                }
            }

            // Show staged boxes summary
            if (stagedBoxes.isNotEmpty()) {
                Text(
                    text = "Pending boxes: " + stagedBoxes.entries.sortedBy { it.key }.joinToString { "#${it.key}=${it.value}" },
                    modifier = Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun BarcodeScannerView(onBarcode: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var permissionDenied by rememberSaveable { mutableStateOf(false) }

    // Permission check
    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            permissionDenied = true
        }
    }

    if (permissionDenied) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Camera permission required") },
            text = { Text("Please grant camera permission in App Settings to scan barcodes.") },
            confirmButton = {}
        )
        return
    }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)

            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                val preview = CameraPreview.Builder().build().apply {
                    setSurfaceProvider(previewView.surfaceProvider)
                }

                val analyzer = ImageAnalysis.Builder()
                    .build()

                val scanner = BarcodeScanning.getClient()

                analyzer.setAnalyzer(ContextCompat.getMainExecutor(ctx)) { imageProxy ->
                    val mediaImage = imageProxy.image
                    if (mediaImage != null) {
                        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                        scanner.process(image)
                            .addOnSuccessListener { barcodes ->
                                val first = barcodes.firstOrNull { it.rawValue != null }
                                if (first?.rawValue != null) {
                                    onBarcode(first.rawValue!!)
                                }
                            }
                            .addOnCompleteListener { imageProxy.close() }
                    } else {
                        imageProxy.close()
                    }
                }

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        analyzer
                    )
                } catch (_: Exception) { }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        },
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.4f)
    )
}

@Composable
fun PackingCreateScreen(onDone: (String) -> Unit, onCancel: () -> Unit) {
    val scope = rememberCoroutineScope()
    val repository = remember { com.encasaxo.hq.data.PackingListRepository() }

    var shipmentId by rememberSaveable { mutableStateOf("") }
    var dispatchDate by rememberSaveable { mutableStateOf("") } // YYYY-MM-DD
    var channelAbb by rememberSaveable { mutableStateOf("") }
    var repositoryVal by rememberSaveable { mutableStateOf("") }
    var production by rememberSaveable { mutableStateOf("") }
    var mode by rememberSaveable { mutableStateOf("") }

    // Dropdown states
    var channelExpanded by rememberSaveable { mutableStateOf(false) }
    var repoExpanded by rememberSaveable { mutableStateOf(false) }
    var productionExpanded by rememberSaveable { mutableStateOf(false) }
    var modeExpanded by rememberSaveable { mutableStateOf(false) }

    val snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
    var saving by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Packing List") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Cancel")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.Top
        ) {
            if (saving) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            OutlinedTextField(
                value = shipmentId,
                onValueChange = { shipmentId = it },
                label = { Text("Shipment ID") },
                modifier = Modifier.fillMaxWidth()
            )

            // Date picker: simple dialog-less input using a clickable field
            OutlinedTextField(
                value = dispatchDate,
                onValueChange = { dispatchDate = it },
                label = { Text("Dispatch Date (YYYY-MM-DD)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
            )

            // Channel dropdown (simple DropdownMenu)
            Box(modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)) {
                OutlinedTextField(
                    value = channelAbb,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Channel") },
                    trailingIcon = {
                        IconButton(onClick = { channelExpanded = !channelExpanded }) {
                            Icon(imageVector = Icons.Default.ArrowBack, contentDescription = null)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                androidx.compose.material3.DropdownMenu(
                    expanded = channelExpanded,
                    onDismissRequest = { channelExpanded = false }
                ) {
                    listOf("", "Amazon EU", "Amazon IN", "Amazon US", "Amazon UK", "Amazon JP", "Globalbees").forEach { opt ->
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text(if (opt.isEmpty()) "Select Channel" else opt) },
                            onClick = {
                                channelAbb = opt
                                channelExpanded = false
                            }
                        )
                    }
                }
            }

            // Repository dropdown
            Box(modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)) {
                OutlinedTextField(
                    value = repositoryVal,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Repository") },
                    trailingIcon = {
                        IconButton(onClick = { repoExpanded = !repoExpanded }) {
                            Icon(imageVector = Icons.Default.ArrowBack, contentDescription = null)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                androidx.compose.material3.DropdownMenu(
                    expanded = repoExpanded,
                    onDismissRequest = { repoExpanded = false }
                ) {
                    listOf("", "Amazon", "Swiggy", "Zepto").forEach { opt ->
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text(if (opt.isEmpty()) "Select Repository" else opt) },
                            onClick = {
                                repositoryVal = opt
                                repoExpanded = false
                            }
                        )
                    }
                }
            }

            // Production dropdown
            Box(modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)) {
                OutlinedTextField(
                    value = production,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Production") },
                    trailingIcon = {
                        IconButton(onClick = { productionExpanded = !productionExpanded }) {
                            Icon(imageVector = Icons.Default.ArrowBack, contentDescription = null)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                androidx.compose.material3.DropdownMenu(
                    expanded = productionExpanded,
                    onDismissRequest = { productionExpanded = false }
                ) {
                    listOf("", "Mumbai", "Karur").forEach { opt ->
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text(if (opt.isEmpty()) "Select Production" else opt) },
                            onClick = {
                                production = opt
                                productionExpanded = false
                            }
                        )
                    }
                }
            }

            // Mode dropdown
            Box(modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)) {
                OutlinedTextField(
                    value = mode,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Mode") },
                    trailingIcon = {
                        IconButton(onClick = { modeExpanded = !modeExpanded }) {
                            Icon(imageVector = Icons.Default.ArrowBack, contentDescription = null)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                androidx.compose.material3.DropdownMenu(
                    expanded = modeExpanded,
                    onDismissRequest = { modeExpanded = false }
                ) {
                    listOf("", "Road", "Sea", "Air").forEach { opt ->
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text(if (opt.isEmpty()) "Select Mode" else opt) },
                            onClick = {
                                mode = opt
                                modeExpanded = false
                            }
                        )
                    }
                }
            }

            Button(
                onClick = {
                    scope.launch {
                        if (shipmentId.isBlank()) {
                            snackbarHostState.showSnackbar("Shipment ID is required")
                            return@launch
                        }
                        saving = true
                        val body = CreatePackingListBody(
                            form = PackingForm(
                                shipmentId = shipmentId,
                                dispatchDate = dispatchDate.ifBlank { null },
                                channelAbb = channelAbb.ifBlank { null },
                                repository = repositoryVal.ifBlank { null },
                                production = production.ifBlank { null },
                                mode = mode.ifBlank { null },
                                status = null
                            ),
                            items = emptyList(),
                            boxDetails = emptyList()
                        )
                        val resp = repository.create(body)
                        saving = false
                        if (resp.isSuccessful) {
                            snackbarHostState.showSnackbar("Created")
                            onDone(shipmentId)
                        } else {
                            snackbarHostState.showSnackbar("Create failed: ${resp.code()}")
                        }
                    }
                },
                modifier = Modifier
                    .padding(top = 16.dp)
                    .fillMaxWidth()
            ) {
                Text("Save")
            }

            Button(
                onClick = onCancel,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .fillMaxWidth()
            ) {
                Text("Cancel")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewEncasaHQ() {
    // Preview-friendly: create a simple ViewModel via viewModel() — in preview this is okay
    val previewVm: PackingListViewModel = viewModel()
    EncasaHQApp(viewModel = previewVm)
}
