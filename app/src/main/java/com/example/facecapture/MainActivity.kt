package com.example.facecapture




import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.net.toFile
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import coil.size.Size
import com.example.facecapture.ui.theme.FaceCaptureTheme
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import org.tensorflow.lite.Interpreter
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.text.SimpleDateFormat
import java.util.Locale

private lateinit var interpreter: Interpreter

class MainActivity : ComponentActivity() {

//    init {
//
//    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FaceCaptureTheme {
                FaceCaptureApp()
            }
        }
    }
}



enum class AppScreen {
    NAME_INPUT,
    CAMERA_CAPTURE,
    IMAGE_GALLERY,
    PERSONS_LIST,
    VERIFY_PERSON
}

@Composable
fun FaceCaptureApp() {
    var personName by remember { mutableStateOf("") }
    var isCaptureMode by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var currentScreen by remember { mutableStateOf<AppScreen>(AppScreen.NAME_INPUT) }

    when (currentScreen) {
        AppScreen.NAME_INPUT -> {
            NameInputScreen(
                onCaptureClick = { name ->
                    personName = name
                    currentScreen = AppScreen.CAMERA_CAPTURE
                },
                onGalleryClick = { name ->
                    personName = name
                    currentScreen = AppScreen.IMAGE_GALLERY
                },
                onPersonClick = {
                    currentScreen = AppScreen.PERSONS_LIST
                }
            )
        }

        AppScreen.PERSONS_LIST -> {
            PersonListScreen(
                onPersonSelect = { name ->
                    personName = name
                    currentScreen = AppScreen.IMAGE_GALLERY
                },
                onBackClick = {
                    currentScreen = AppScreen.NAME_INPUT
                }
            )
        }

        AppScreen.CAMERA_CAPTURE -> {
            CameraCapture(
                personName = personName,
                onBackClick = {
                    currentScreen = AppScreen.NAME_INPUT
                },
                onCaptureComplete = {
                    currentScreen = AppScreen.NAME_INPUT
                }
            )
        }

        AppScreen.IMAGE_GALLERY -> {
            ImageGalleryScreen(
                personName = personName,
                onBackClick = {
                    currentScreen = AppScreen.PERSONS_LIST
                },
                onVerifyClick = {
                    currentScreen = AppScreen.VERIFY_PERSON
                },
                onAddImageClick = {
                    currentScreen = AppScreen.CAMERA_CAPTURE
                }
            )
        }

        AppScreen.VERIFY_PERSON -> {
            VerifyPerson(
                personName = personName,
                onBackClick = {
                    currentScreen = AppScreen.IMAGE_GALLERY
                },
                onCaptureComplete = {

                }
            )
        }
    }

}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonListScreen(
    onPersonSelect: (String) -> Unit,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current

    val personsList = remember { getPersonList(context) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Capture Persons")
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if(personsList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No persons captured yet",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {

            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
            ) {
                items(personsList) { personName ->
                    PersonListList(
                        personName = personName,
                        onPersonClick = { onPersonSelect(personName)}
                    )

                }
            }
        }
    }
}



@Composable
fun PersonListList(
    personName: String,
    onPersonClick: () -> Unit
) {
    val context = LocalContext.current

    val imageCount = remember {
        getPersonImageCount(context = context, personName = personName)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onPersonClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Person,
            contentDescription = "Person",
            modifier = Modifier.size(40.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column {
            Text(
                text = personName,
                style = MaterialTheme.typography.bodyLarge
            )

            Text(
                text = "$imageCount images captured",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

}

fun getPersonList(context: Context): List<String> {
    val baseDir = File(context.getExternalFilesDir(null), context.getString(R.string.facecaptures))

    return baseDir.listFiles()
        ?.filter { it.isDirectory }
        ?.map { it.name }
        ?: emptyList()
}

fun getPersonImageCount(context: Context, personName: String): Int {

    val personDir = File(context.getExternalFilesDir(null), context.getString(R.string.facecaptures))

    return personDir.listFiles()
        ?.filter { it.extension == "jpg" }
        ?.size
        ?: 0
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageGalleryScreen(
    personName: String,
    onBackClick: () -> Unit,
    onVerifyClick: () -> Unit,
    onAddImageClick: () -> Unit
) {
    val context = LocalContext.current
    var images by remember { mutableStateOf(listOf<File>()) }
    var selectedImage by remember { mutableStateOf<File?>(null) }



    LaunchedEffect(personName) {
        val personDir = File(context.getExternalFilesDir(null), "FaceCaptures/$personName")
        images = personDir.listFiles()?.filter { it.extension == "jpg" } ?: listOf()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("$personName's Captured Faces") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar {
                Button(
                    onClick = onAddImageClick,
                    modifier = Modifier
                        .padding(16.dp, 0.dp)
                ) {
                    Text(
                        text = "Add Image"
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            Row {
                Text(
                    text = "Total Captures: ${images.size}",
                    modifier = Modifier.padding(16.dp)
                )

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = onVerifyClick,
                    modifier = Modifier.padding(16.dp, 0.dp)
                ) {
                    Text(
                        text = "Verify"
                    )
                }
            }


            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(8.dp)
            ) {
                items(images) { imagesFile ->
                    ImageThumbnail(
                        imageFile = imagesFile,
                        onClick = { selectedImage = imagesFile }
                    )
                }
            }

            selectedImage?.let { file ->
                ImagePreviewDialog(
                    imageFile = file,
                    onDismiss = { selectedImage = null },
                    onDelete = {
                        file.delete()
                        images = images.filter { it != file}
                        selectedImage = null
                    }
                )
            }
        }
    }
}


@Composable
fun ImageThumbnail(
    imageFile: File,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .padding(4.dp)
            .aspectRatio(1f)
            .clickable(onClick = onClick)
    ) {
        LocalImageLoader(
            imageFile = imageFile,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

    }
}


@androidx.annotation.OptIn(ExperimentalGetImage::class)
@Composable
fun VerifyPerson(
    personName: String,
    onBackClick: () -> Unit,
    onCaptureComplete: () -> Unit
) {
    val context = LocalContext.current

    loadModel(context)

    val controller = remember { LifecycleCameraController(context) }
    var capturedImageUrl by remember { mutableStateOf<Uri?>(null) }

    var isFrontCamera by remember { mutableStateOf(true) }

    var faceBoxes by remember { mutableStateOf<List<RectF>>(emptyList()) }
    val faceDetector = remember {
        FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setContourMode(FaceDetectorOptions.CONTOUR_MODE_NONE)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
                .setMinFaceSize(0.1f)
                .build()
        )
    }

    val faceOverlayView = remember { FaceOverlayView(context) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->

        if(!isGranted) {
            onBackClick()
        }
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    LaunchedEffect(controller, isFrontCamera) {
        controller.setCameraSelector(
            if (isFrontCamera) CameraSelector.DEFAULT_FRONT_CAMERA
            else CameraSelector.DEFAULT_BACK_CAMERA
        )
    }

    LaunchedEffect(controller) {
        controller.setImageAnalysisAnalyzer(
            ContextCompat.getMainExecutor(context)
        ) { imageProxy: ImageProxy ->
            val mediaImage = imageProxy.image
            if (mediaImage != null) {
                val image = InputImage.fromMediaImage(
                    mediaImage,
                    imageProxy.imageInfo.rotationDegrees
                )

                faceDetector.process(image)
                    .addOnSuccessListener { faces ->
                        val relativeBoxes = faces.map { face ->
                            val box = face.boundingBox


                            val imageWidth = mediaImage.width.toFloat()
                            val imageHeight = mediaImage.height.toFloat()

                            val left = box.left / (imageWidth)
                            val top = box.top / (imageHeight)
                            val right = box.right / imageWidth
                            val bottom = box.bottom / imageHeight

                            RectF(left, top, right, bottom)


                        }

                        faceBoxes = relativeBoxes
                        faceOverlayView.updateFaces(relativeBoxes, isFrontCamera)
                    }
                    .addOnFailureListener { e ->
                        Log.e("FaceDetection", "Face detection failed", e)
                    }
                    .addOnCompleteListener {
                        imageProxy.close()
                    }
            } else {
                imageProxy.close()
            }

        }
    }
    var output by remember { mutableDoubleStateOf(0.0) }
    if (capturedImageUrl == null) {



        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AndroidView(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                factory = { context ->
                    controller.bindToLifecycle(context as ComponentActivity)
                    val previewView = PreviewView(context)
                    previewView.controller = controller
                    previewView
                }
            )

            Text(
                text = "${faceBoxes.size} ${if (faceBoxes.size == 1) "face" else "faces"} detected",
                color = Color.White,
                modifier = Modifier
                    .padding(8.dp)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(4.dp)
            )


            Button(
                onClick = {

                    captureImage(
                        context = context,
                        controller = controller,
                        personName = personName
                    ) { uri ->
                        capturedImageUrl = uri
                        Toast.makeText(context, "Please wait face is getting verify", Toast.LENGTH_LONG).show()
                        output = VerifyFaces(uri.toFile(), personName, context)
                    }

                },
                enabled = if (faceBoxes.size == 1) true else false,
                modifier = Modifier.padding(16.dp)
            ) {
                Text("Verify")
            }

            if (capturedImageUrl != null) {
                Toast.makeText(context, "Captured the image $capturedImageUrl", Toast.LENGTH_LONG).show()
            }

            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = { isFrontCamera = !isFrontCamera },
                ) {
                    Text("Switch Camera")
                }

                Button(onClick = onBackClick) {
                    Text("Back")
                }
            }


            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween

            ) {

            }

        }

    }
    if (capturedImageUrl != null) {

        var vertifing by remember { mutableStateOf(false) }


       Column (
           modifier = Modifier
               .fillMaxSize(),
           horizontalAlignment = Alignment.CenterHorizontally,
           verticalArrangement = Arrangement.Center
       ) {

           val painter = rememberAsyncImagePainter(
               model = ImageRequest.Builder(LocalContext.current)
                   .data(capturedImageUrl)
                   .size(Size.ORIGINAL) // Load the image at its original size
                   .build()
           )

           Image(
               painter = painter,
               contentDescription = "Captured Image",
               modifier = Modifier
                   .size(359.dp)
                   .rotate(degrees = -90f),
               contentScale = ContentScale.Fit
           )

           Row(
               horizontalArrangement = Arrangement.SpaceEvenly,
               modifier = Modifier
                   .fillMaxWidth()
           ) {
//               Button(
//                   onClick = {
//                       vertifing = true
//                       output = VerifyFaces(capturedImageUrl!!.toFile(), personName, context)
//                   },
//                   enabled = (!vertifing)
//               ) {
//                   Text(
//                       "Verify"
//                   )
//               }

               Button(
                   onClick = {
                       capturedImageUrl!!.path?.let { File(it).delete() }
                       capturedImageUrl = null
                   },
                   enabled = (!vertifing)
               ) {
                   Text(
                       "Retry"
                   )
               }

               Button(
                   onClick = onBackClick,
                   enabled = (!vertifing)
               ) {
                   Text("Back")
               }
           }


           if(output != 0.0) {
               vertifing = false
               Text(
                   text = "Face is ${if (output > 0.5) "match by $output accuracy" else "not match"}",
                   modifier = Modifier
                       .fillMaxWidth(),
                   textAlign = TextAlign.Center
               )
           } else {
//               Text(
//                   text = "Please wait 5 second after you click on verify button"
//               )
           }
       }


    }
}

@Composable
fun ImagePreviewDialog(
    imageFile: File,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Image Preview") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                LocalImageLoader(
                    imageFile = imageFile,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f),
                    contentScale = ContentScale.Fit
                )

                Text(
                    text = "Filename : ${imageFile.name}",
                    modifier = Modifier
                        .padding(top = 8.dp)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text("Close")
            }
        },
        dismissButton = {
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete Image",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    )
}

@Composable
fun LocalImageLoader(
    imageFile: File,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val bitmap = remember() {
        BitmapFactory.decodeFile(imageFile.absolutePath)?.asImageBitmap()
    }

    bitmap?.let { loadedBitmap ->
        Image(
            bitmap = loadedBitmap,
            contentDescription = "Captured Image",
            modifier = modifier,
            contentScale = contentScale
        )
    }
}


@androidx.annotation.OptIn(ExperimentalGetImage::class)
@Composable
fun CameraCapture(
    personName: String,
    onBackClick: () -> Unit,
    onCaptureComplete: () -> Unit
) {
    val context = LocalContext.current
    val controller = remember { LifecycleCameraController(context) }
    var capturedImageUrl by remember { mutableStateOf<Uri?>(null) }
    var captureCount by remember { mutableStateOf(0) }

    var isFrontCamera by remember { mutableStateOf(true) }

    var faceBoxes by remember { mutableStateOf<List<RectF>>(emptyList()) }
    val faceDetector = remember {
        FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setContourMode(FaceDetectorOptions.CONTOUR_MODE_NONE)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
                .setMinFaceSize(0.1f)
                .build()
        )
    }

    val faceOverlayView = remember { FaceOverlayView(context) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->

        if(!isGranted) {
            onBackClick()
        }
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    LaunchedEffect(controller, isFrontCamera) {
        controller.setCameraSelector(
            if (isFrontCamera) CameraSelector.DEFAULT_FRONT_CAMERA
            else CameraSelector.DEFAULT_BACK_CAMERA
        )
    }

    LaunchedEffect(controller) {
        controller.setImageAnalysisAnalyzer(
            ContextCompat.getMainExecutor(context)
        ) { imageProxy: ImageProxy ->
            val mediaImage = imageProxy.image
            if (mediaImage != null) {
                val image = InputImage.fromMediaImage(
                    mediaImage,
                    imageProxy.imageInfo.rotationDegrees
                )

                faceDetector.process(image)
                    .addOnSuccessListener { faces ->
                        val relativeBoxes = faces.map { face ->
                            val box = face.boundingBox


                            val imageWidth = mediaImage.width.toFloat()
                            val imageHeight = mediaImage.height.toFloat()

                            val left = box.left / (imageWidth)
                            val top = box.top / (imageHeight)
                            val right = box.right / imageWidth
                            val bottom = box.bottom / imageHeight

                            RectF(left, top, right, bottom)


                        }

                        faceBoxes = relativeBoxes
                        faceOverlayView.updateFaces(relativeBoxes, isFrontCamera)

                    }
                    .addOnFailureListener { e ->
                        Log.e("FaceDetection", "Face detection failed", e)
                    }
                    .addOnCompleteListener {
                        imageProxy.close()
                    }
            } else {
                imageProxy.close()
            }

        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AndroidView(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            factory = { context ->
                controller.bindToLifecycle(context as ComponentActivity)
//                val previewView = PreviewView(context)
//                previewView.controller = controller
//                previewView
                val previewView = PreviewView(context)
                previewView.controller = controller
                previewView

                /*val previewView = PreviewView(context).apply {
                    implementationMode = PreviewView.ImplementationMode.PERFORMANCE
                    controller = controller.apply {
                        bindToLifecycle(context as ComponentActivity)
                    }
                }*/

//                val container = FrameLayout(context).apply {
//                    layoutParams = ViewGroup.LayoutParams(
//                        ViewGroup.LayoutParams.MATCH_PARENT,
//                        ViewGroup.LayoutParams.MATCH_PARENT
//                    )
//
//                    addView(previewView)
//                    addView(faceOverlayView, ViewGroup.LayoutParams(
//                        ViewGroup.LayoutParams.MATCH_PARENT,
//                        ViewGroup.LayoutParams.MATCH_PARENT
//                    ))
//                }
//
//                container
            }
        )

        if (faceBoxes.isNotEmpty()) {
            Text(
                text = "${faceBoxes.size} ${if (faceBoxes.size == 1) "face" else "faces"} detected",
                color = Color.White,
                modifier = Modifier
                    .padding(8.dp)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(4.dp)
            )
        }

        Button(
            onClick = {
                captureImage(
                    context = context,
                    controller = controller,
                    personName = personName
                ) { uri ->
                    capturedImageUrl = uri
                    captureCount++
                }
            },
            enabled = if (faceBoxes.size == 1) true else false,
            modifier = Modifier.padding(16.dp)
        ) {
            Text("Capture Face")
        }

        Row {
            Button(
                onClick = { isFrontCamera = !isFrontCamera },
                modifier = Modifier.padding(16.dp)
            ) {
                Text("Switch Camera")
            }
        }


        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Captured Images: $captureCount")

            Button(onClick = onBackClick) {
                Text("Back")
            }
        }

        Button(
            onClick = {
                onCaptureComplete()
            },
            enabled = if (captureCount >= 10) true else false,
            modifier = Modifier.padding((16.dp))
        ) {
            Text("Complete Capture")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NameInputScreen(
    onCaptureClick: (String) -> Unit,
    onGalleryClick: (String) -> Unit,
    onPersonClick: () -> Unit
) {
    var personName by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Face Capture App") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            TextField(
                value = personName,
                onValueChange = { personName = it },
                label = { Text("Enter Person's Name") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Column (
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        if (personName.isNotBlank()) {
                            onCaptureClick(personName)
                        }
                    },
                    enabled = personName.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Start Capturing")
                }

                Button(
                    onClick = onPersonClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Persons List")
                }
            }

        }
    }
}

fun captureImage(
    context: Context,
    controller: LifecycleCameraController,
    personName: String,
    onImageCapture: (Uri) -> Unit
) {

    val personDir = File(context.getExternalFilesDir(null), "FaceCaptures/$personName")
    personDir.mkdirs()

    val filename = personName.replace(" ","") + "_" + SimpleDateFormat(
        "yyyy-MM-dd-HH-mm-ss-SSS",
        Locale.US
    ).format(System.currentTimeMillis()) + ".jpg"

    val outputOptions = ImageCapture.OutputFileOptions.Builder(
        File(personDir, filename)
    ).build()

    controller.takePicture(
        outputOptions,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {

                val originalUri = output.savedUri?.path?.let { File(it) } ?: File(personDir, filename)
                val savedUri = Uri.fromFile(output.savedUri?.path?.let { File(it) } ?: File(personDir, filename) )
                val savedUri2 = resizeImage(context, Uri.fromFile(originalUri), personDir, filename)
                onImageCapture(savedUri2)

                Log.d("SaveUrl 1", "$savedUri")
                Log.d("SaveUrl 2", "$savedUri2")
            }

            override fun onError(exception: ImageCaptureException) {
                Log.e("CameraCapture", "Image Capture Failed", exception)
            }
        }
    )
}


private fun resizeImage(
    context: Context,
    originalUri: Uri,
    outputDir: File,
    filename: String
) : Uri {
    try {
        val inputStream = context.contentResolver.openInputStream(originalUri)
        val originalBitmap = BitmapFactory.decodeStream(inputStream)
        inputStream?.close()

        val originalWidth = originalBitmap.width
        val originalHeight = originalBitmap.height

        val targetSize = 350

        val newWidth: Int
        val newHeight: Int

        if (originalWidth > originalHeight) {
            newWidth = targetSize
            newHeight = (originalHeight * targetSize) / originalWidth
        } else {
            newHeight = targetSize
            newWidth = (originalWidth * targetSize) / originalHeight
        }

        val resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true)

        originalBitmap.recycle()

        val resizedFile = File(outputDir, "resized_$filename")
        val outputStream = FileOutputStream(resizedFile)

        resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)

        outputStream.flush()
        outputStream.close()

        resizedBitmap.recycle()

        originalUri.path?.let { File(it).delete() }

        return Uri.fromFile(resizedFile)
    } catch (e: Exception) {
        Log.e("ImageResizing", "Failed to resize image", e)
        return originalUri
    }
}

class FaceOverlayView(context: Context) : android.view.View(context) {
    private val rectanglePaint = Paint().apply {
        color = Color.Red.toArgb()
        style = Paint.Style.STROKE
        strokeWidth = 8f
    }

    private var faceBoxes = listOf<RectF>()
    private var isFrontCamera = true

    fun updateFaces(boxes: List<RectF>, isFront: Boolean) {
        faceBoxes = boxes
        isFrontCamera = isFront
        invalidate()
    }



    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (faceBoxes.isEmpty()) return

        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()

        for (box in faceBoxes) {

            val left = if (isFrontCamera) viewWidth * (1 - box.right) else viewWidth * box.left
            val right = if (isFrontCamera) viewWidth * (1- box.left) else viewWidth * box.right
            val top = viewHeight * box.top
            val bottom = viewHeight * box.bottom

            canvas.drawRect(left, top, right, bottom, rectanglePaint)
        }
    }
}


private fun loadModel(context: Context) {
    val modelFile = "saimese-model-lite.tflite"
    val fileDescriptor = context.assets.openFd(modelFile)
    val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
    val fileChannel = inputStream.channel
    val startOffset = fileDescriptor.startOffset
    val declaredLenght = fileDescriptor.declaredLength
    val mappedByteBuffer = fileChannel.map(
        FileChannel.MapMode.READ_ONLY,
        startOffset,
        declaredLenght
    )

    val options = Interpreter.Options()
    interpreter = Interpreter(mappedByteBuffer, options)
}

private fun preprcessImage(bitmap: Bitmap): ByteBuffer {
    val inputWidth = 105
    val inputHeight = 105
    val resizedBitmap = Bitmap.createScaledBitmap(bitmap, inputWidth, inputHeight, true)

    val inputBuffer = ByteBuffer.allocateDirect(
        inputWidth * inputHeight * 4 * 3
    ).apply {
        order(ByteOrder.nativeOrder())
    }

    val intValues = IntArray(inputWidth * inputHeight)
    resizedBitmap.getPixels(intValues, 0, resizedBitmap.width, 0, 0, resizedBitmap.width, resizedBitmap.height)

    for (pixelValue in intValues) {
        val r = (pixelValue shr 16 and 0xFF) / 255.0f
        val g = (pixelValue shr 8 and 0xFF) / 255.0f
        val b = (pixelValue and 0xFF) / 255.0f

        inputBuffer.putFloat(r)
        inputBuffer.putFloat(g)
        inputBuffer.putFloat(b)
    }

    return inputBuffer
}

private fun runInference(face1: Bitmap, face2: Bitmap): Float {

    val inputBuffer1 = preprcessImage(face1)
    val inputStream2 = preprcessImage(face2)

    val outputBuffer = ByteBuffer.allocateDirect(4).apply {
        order(ByteOrder.nativeOrder())
    }

    val inputs = arrayOf(inputBuffer1, inputStream2)
    val outputs = mapOf(0 to outputBuffer)


    interpreter.runForMultipleInputsOutputs(inputs, outputs)

    outputBuffer.rewind()
    return outputBuffer.getFloat()
}



fun VerifyFaces(
    anchorImage: File,
    personName: String,
    context: Context
): Double {
    Log.d("Output", "Wait")
    val personDir = File(context.getExternalFilesDir(null), "FaceCaptures/$personName")
    val images: List<File> = personDir.listFiles()?.filter { it.extension == "jpg" } ?: listOf()


    val outputs = mutableListOf<Float>()
    var count = 1
    for (image in images) {
        if (count > 10) {
            break
        }
        val output:Float = runInference(BitmapFactory.decodeFile(anchorImage.absolutePath), BitmapFactory.decodeFile(image.absolutePath))
        outputs.add(if (output > 0.5f) output else 0f)
        count++
    }

    Log.d("Output", "Output : ${outputs.average()}")
    return outputs.average()
}

