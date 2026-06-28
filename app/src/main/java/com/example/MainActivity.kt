package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.AppDatabase
import com.example.data.Note
import com.example.data.PlannerRepository
import com.example.data.Task
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.LanguageCode
import com.example.ui.PlannerTab
import com.example.ui.PlannerViewModel
import com.example.ui.PlannerViewModelFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize local Room database and repository
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = PlannerRepository(database.taskDao(), database.noteDao())

        // Create ViewModel using our custom factory
        val viewModel: PlannerViewModel by viewModels {
            PlannerViewModelFactory(repository)
        }

        setContent {
            MyApplicationTheme {
                PlannerAppScreen(viewModel)
            }
        }
    }
}

// Available colors for customizing notes
val NoteColors = listOf(
    "#FFCDD2", // Pastel Red
    "#E8AEFF", // Pastel Purple
    "#B3E5FC", // Pastel Blue
    "#C8E6C9", // Pastel Green
    "#FFF9C4", // Pastel Yellow
    "#FFE0B2", // Pastel Orange
    "#F5F5F5"  // Pastel Gray
)

enum class SheetType {
    ADD_TASK, EDIT_TASK, ADD_NOTE, EDIT_NOTE
}

@Composable
fun PlannerAppScreen(viewModel: PlannerViewModel) {
    // Collect reactive state from our ViewModel
    val language by viewModel.languageState.collectAsStateWithLifecycle()
    val activeTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategoryFilter.collectAsStateWithLifecycle()
    val query by viewModel.searchQuery.collectAsStateWithLifecycle()
    val tasks by viewModel.tasksState.collectAsStateWithLifecycle()
    val notes by viewModel.notesState.collectAsStateWithLifecycle()
    val progress by viewModel.taskProgress.collectAsStateWithLifecycle()

    // Sheet and dialog state
    var showSheet by remember { mutableStateOf(false) }
    var sheetType by remember { mutableStateOf(SheetType.ADD_TASK) }
    
    // Form fields state
    var taskTitle by remember { mutableStateOf("") }
    var taskCategory by remember { mutableStateOf("Personal") }
    var taskPriority by remember { mutableStateOf("Medium") }
    var activeEditingTask by remember { mutableStateOf<Task?>(null) }

    var noteTitle by remember { mutableStateOf("") }
    var noteContent by remember { mutableStateOf("") }
    var noteColor by remember { mutableStateOf("#F5F5F5") }
    var activeEditingNote by remember { mutableStateOf<Note?>(null) }

    // Translation helpers
    fun t(bn: String, en: String): String = viewModel.translate(bn, en)

    Scaffold(
        modifier = Modifier.fillMaxSize().testTag("scaffold_root"),
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (activeTab == PlannerTab.TASKS) {
                        taskTitle = ""
                        taskCategory = if (selectedCategory == "All") "Personal" else selectedCategory
                        taskPriority = "Medium"
                        sheetType = SheetType.ADD_TASK
                    } else {
                        noteTitle = ""
                        noteContent = ""
                        noteColor = NoteColors.first()
                        sheetType = SheetType.ADD_NOTE
                    }
                    showSheet = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_item_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = t("নতুন যোগ করুন", "Add New")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = innerPadding.calculateBottomPadding()),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                // 1. Beautiful Hero Banner with overlay
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_banner),
                            contentDescription = "Planner Banner",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        // Gradient translucent overlay
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Black.copy(alpha = 0.4f),
                                            Color.Black.copy(alpha = 0.7f)
                                        )
                                    )
                                )
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Top row with English/Bengali language switcher
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = t("কাজ ও নোট প্ল্যানার", "Task & Notes Planner"),
                                        color = Color.White,
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    val currentDateStr = SimpleDateFormat(
                                        t("dd MMMM, yyyy", "MMMM dd, yyyy"),
                                        if (language == LanguageCode.BN) Locale("bn") else Locale.ENGLISH
                                    ).format(Date())
                                    Text(
                                        text = currentDateStr,
                                        color = Color.White.copy(alpha = 0.8f),
                                        fontSize = 12.sp
                                    )
                                }

                                // Interactive Language toggle button
                                Button(
                                    onClick = { viewModel.toggleLanguage() },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.White.copy(alpha = 0.25f),
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("language_toggle_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Language,
                                        contentDescription = "Language toggle",
                                        modifier = Modifier.size(16.dp).padding(end = 4.dp)
                                    )
                                    Text(
                                        text = if (language == LanguageCode.BN) "English" else "বাংলা",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            // Completion Progress indicator card overlay
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = Color.White.copy(alpha = 0.15f)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    val completed = progress.first
                                    val total = progress.second
                                    
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        CircularProgressIndicator(
                                            progress = if (total > 0) completed.toFloat() / total else 0f,
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            trackColor = Color.White.copy(alpha = 0.2f),
                                            strokeWidth = 3.dp,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        Text(
                                            text = if (total > 0) "${(completed * 100) / total}%" else "0%",
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Column {
                                        Text(
                                            text = t("আজকের অগ্রগতি", "Today's Progress"),
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                        val progressText = if (total == 0) {
                                            t("আজকের কোনো কাজ নেই। কিছু যোগ করুন!", "No tasks for today. Add some!")
                                        } else if (completed == total) {
                                            t("অসাধারণ! সব কাজ সম্পন্ন হয়েছে!", "Amazing! All tasks completed!")
                                        } else {
                                            String.format(
                                                t("আপনি %2\$dটির মধ্যে %1\$dটি কাজ সম্পন্ন করেছেন", "You completed %1\$d of %2\$d tasks"),
                                                completed, total
                                            )
                                        }
                                        Text(
                                            text = progressText,
                                            color = Color.White.copy(alpha = 0.85f),
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. Search Box
                item {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { viewModel.searchQuery.value = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .testTag("search_input"),
                        placeholder = {
                            Text(
                                text = t("কাজ বা নোট খুঁজুন...", "Search tasks or notes..."),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search"
                            )
                        },
                        trailingIcon = {
                            if (query.isNotEmpty()) {
                                IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear search"
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                }

                // 3. Elegant Tab Layout for Tasks & Notes
                item {
                    TabRow(
                        selectedTabIndex = if (activeTab == PlannerTab.TASKS) 0 else 1,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp).clip(RoundedCornerShape(16.dp)),
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        indicator = @Composable { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[if (activeTab == PlannerTab.TASKS) 0 else 1]),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    ) {
                        Tab(
                            selected = activeTab == PlannerTab.TASKS,
                            onClick = { viewModel.currentTab.value = PlannerTab.TASKS },
                            modifier = Modifier.testTag("tab_button_tasks")
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (activeTab == PlannerTab.TASKS) Icons.Filled.List else Icons.Outlined.List,
                                    contentDescription = "Tasks tab",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = t("কাজসমূহ", "Tasks"),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                        Tab(
                            selected = activeTab == PlannerTab.NOTES,
                            onClick = { viewModel.currentTab.value = PlannerTab.NOTES },
                            modifier = Modifier.testTag("tab_button_notes")
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (activeTab == PlannerTab.NOTES) Icons.Filled.Description else Icons.Outlined.Description,
                                    contentDescription = "Notes tab",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = t("নোটসমূহ", "Notes"),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }

                // Tab Content Rendering
                if (activeTab == PlannerTab.TASKS) {
                    // Category Filters Row for Tasks
                    item {
                        val categories = listOf("All", "Personal", "Work", "Shopping", "Health")
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            categories.forEach { cat ->
                                val categoryDisplayName = when (cat) {
                                    "All" -> t("সব", "All")
                                    "Personal" -> t("ব্যক্তিগত", "Personal")
                                    "Work" -> t("কাজ", "Work")
                                    "Shopping" -> t("কেনাকাটা", "Shopping")
                                    "Health" -> t("স্বাস্থ্য", "Health")
                                    else -> cat
                                }
                                val isSelected = selectedCategory == cat
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.selectedCategoryFilter.value = cat },
                                    label = { Text(categoryDisplayName, fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                        containerColor = MaterialTheme.colorScheme.surface,
                                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    modifier = Modifier.testTag("category_chip_$cat")
                                )
                            }
                        }
                    }

                    // Task List Items
                    if (tasks.isEmpty()) {
                        item {
                            EmptyStateView(
                                icon = Icons.Outlined.AssignmentLate,
                                message = t(
                                    "এখানে কোনো কাজ নেই। যোগ করতে নিচের + বোতামটি চাপুন!",
                                    "No tasks here. Tap the + button below to create one!"
                                )
                            )
                        }
                    } else {
                        items(tasks, key = { it.id }) { task ->
                            TaskItemCard(
                                task = task,
                                onToggle = { viewModel.toggleTaskCompletion(task) },
                                onDelete = { viewModel.deleteTaskById(task.id) },
                                onClick = {
                                    activeEditingTask = task
                                    taskTitle = task.title
                                    taskCategory = task.category
                                    taskPriority = task.priority
                                    sheetType = SheetType.EDIT_TASK
                                    showSheet = true
                                },
                                translate = { bn, en -> t(bn, en) }
                            )
                        }
                    }
                } else {
                    // Notes tab: displays custom responsive 2-column layout safely
                    if (notes.isEmpty()) {
                        item {
                            EmptyStateView(
                                icon = Icons.Outlined.EditNote,
                                message = t(
                                    "এখানে কোনো নোট নেই। আপনার চিন্তাগুলো লিখে রাখুন!",
                                    "No notes here. Write down your ideas!"
                                )
                            )
                        }
                    } else {
                        // Chunk notes in pairs to render them side-by-side cleanly and adaptively
                        val chunkedNotes = notes.chunked(2)
                        items(chunkedNotes) { rowNotes ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                for (note in rowNotes) {
                                    NoteItemCard(
                                        note = note,
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("note_item_card_${note.id}"),
                                        onClick = {
                                            activeEditingNote = note
                                            noteTitle = note.title
                                            noteContent = note.content
                                            noteColor = note.colorHex
                                            sheetType = SheetType.EDIT_NOTE
                                            showSheet = true
                                        },
                                        onDelete = { viewModel.deleteNoteById(note.id) }
                                    )
                                }
                                // Fill empty space in the grid row if odd number of items
                                if (rowNotes.size < 2) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }

            // 4. Custom Elegant Animated Bottom Sheet Overlay
            AnimatedVisibility(
                visible = showSheet,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .clickable { showSheet = false },
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = false) { } // Prevent background click from dismissing
                            .navigationBarsPadding() // Support system gesture bar padding
                            .imePadding(), // Adjust automatically when soft keyboard opens
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Drag Handle Bar for visual polish
                            Box(
                                modifier = Modifier
                                    .size(width = 40.dp, height = 4.dp)
                                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), CircleShape)
                                    .align(Alignment.CenterHorizontally)
                            )

                            // Title of Bottom Sheet
                            val sheetHeaderTitle = when (sheetType) {
                                SheetType.ADD_TASK -> t("নতুন কাজ যোগ করুন", "Add New Task")
                                SheetType.EDIT_TASK -> t("কাজ সম্পাদনা করুন", "Edit Task")
                                SheetType.ADD_NOTE -> t("নতুন নোট যোগ করুন", "Add New Note")
                                SheetType.EDIT_NOTE -> t("নোট সম্পাদনা করুন", "Edit Note")
                            }

                            Text(
                                text = sheetHeaderTitle,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            // Fields rendering based on Sheet Type
                            if (sheetType == SheetType.ADD_TASK || sheetType == SheetType.EDIT_TASK) {
                                // TASK FIELDS
                                OutlinedTextField(
                                    value = taskTitle,
                                    onValueChange = { taskTitle = it },
                                    label = { Text(t("কাজের শিরোনাম", "Task Title")) },
                                    placeholder = { Text(t("কি করতে হবে?", "What needs to be done?")) },
                                    modifier = Modifier.fillMaxWidth().testTag("sheet_task_title_input"),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp)
                                )

                                // Category Selection Pill Buttons
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = t("ক্যাটাগরি", "Category"),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    val taskCategories = listOf("Personal", "Work", "Shopping", "Health")
                                    Row(
                                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        taskCategories.forEach { cat ->
                                            val displayName = when (cat) {
                                                "Personal" -> t("ব্যক্তিগত", "Personal")
                                                "Work" -> t("কাজ", "Work")
                                                "Shopping" -> t("কেনাকাটা", "Shopping")
                                                "Health" -> t("স্বাস্থ্য", "Health")
                                                else -> cat
                                            }
                                            val selected = taskCategory == cat
                                            ElevatedFilterChip(
                                                selected = selected,
                                                onClick = { taskCategory = cat },
                                                label = { Text(displayName, fontSize = 12.sp) }
                                            )
                                        }
                                    }
                                }

                                // Priority Level Row
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = t("অগ্রাধিকার", "Priority"),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    val priorities = listOf("High", "Medium", "Low")
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        priorities.forEach { prio ->
                                            val displayName = when (prio) {
                                                "High" -> t("উচ্চ", "High")
                                                "Medium" -> t("মাঝারি", "Medium")
                                                "Low" -> t("নিম্ন", "Low")
                                                else -> prio
                                            }
                                            val selected = taskPriority == prio
                                            val color = when (prio) {
                                                "High" -> Color(0xFFEF5350)
                                                "Medium" -> Color(0xFFFFB74D)
                                                else -> Color(0xFF4DB6AC)
                                            }
                                            Button(
                                                onClick = { taskPriority = prio },
                                                modifier = Modifier.weight(1f),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (selected) color else MaterialTheme.colorScheme.surfaceVariant,
                                                    contentColor = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                                ),
                                                shape = RoundedCornerShape(12.dp),
                                                contentPadding = PaddingValues(vertical = 8.dp)
                                            ) {
                                                Text(displayName, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            } else {
                                // NOTE FIELDS
                                OutlinedTextField(
                                    value = noteTitle,
                                    onValueChange = { noteTitle = it },
                                    label = { Text(t("শিরোনাম", "Title")) },
                                    placeholder = { Text(t("নোটের শিরোনাম লিখুন", "Enter note title")) },
                                    modifier = Modifier.fillMaxWidth().testTag("sheet_note_title_input"),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp)
                                )

                                OutlinedTextField(
                                    value = noteContent,
                                    onValueChange = { noteContent = it },
                                    label = { Text(t("মূল বিষয়", "Content")) },
                                    placeholder = { Text(t("আপনার চিন্তা বা তথ্য লিখুন...", "Write your thoughts or info...")) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(120.dp)
                                        .testTag("sheet_note_content_input"),
                                    shape = RoundedCornerShape(12.dp)
                                )

                                // Color Selector Circles Row
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = t("রঙ নির্বাচন", "Highlight Color"),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        NoteColors.forEach { hex ->
                                            val color = Color(android.graphics.Color.parseColor(hex))
                                            val isSelected = noteColor == hex
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(CircleShape)
                                                    .background(color)
                                                    .border(
                                                        width = if (isSelected) 3.dp else 1.dp,
                                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.3f),
                                                        shape = CircleShape
                                                    )
                                                    .clickable { noteColor = hex },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (isSelected) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = "Selected",
                                                        tint = Color.DarkGray,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Bottom actions save and cancel
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { showSheet = false },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("cancel_button"),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(t("বাতিল", "Cancel"))
                                }

                                Button(
                                    onClick = {
                                        if (sheetType == SheetType.ADD_TASK) {
                                            viewModel.addTask(taskTitle, taskCategory, taskPriority)
                                        } else if (sheetType == SheetType.EDIT_TASK) {
                                            activeEditingTask?.let {
                                                viewModel.updateTask(
                                                    it.copy(
                                                        title = taskTitle.trim(),
                                                        category = taskCategory,
                                                        priority = taskPriority
                                                    )
                                                )
                                            }
                                        } else if (sheetType == SheetType.ADD_NOTE) {
                                            viewModel.addNote(noteTitle, noteContent, noteColor)
                                        } else if (sheetType == SheetType.EDIT_NOTE) {
                                            activeEditingNote?.let {
                                                viewModel.updateNote(
                                                    it.copy(
                                                        title = noteTitle.trim(),
                                                        content = noteContent.trim(),
                                                        colorHex = noteColor
                                                    )
                                                )
                                            }
                                        }
                                        showSheet = false
                                    },
                                    enabled = if (sheetType == SheetType.ADD_TASK || sheetType == SheetType.EDIT_TASK) {
                                        taskTitle.isNotBlank()
                                    } else {
                                        noteTitle.isNotBlank() || noteContent.isNotBlank()
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("save_button"),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(t("সংরক্ষণ", "Save"))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Beautiful Empty State Composable
@Composable
fun EmptyStateView(icon: androidx.compose.ui.graphics.vector.ImageVector, message: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 48.dp, bottom = 48.dp, start = 32.dp, end = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = "Empty list illustration",
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            lineHeight = 20.sp
        )
    }
}

// Stunning Individual Task Item Card
@Composable
fun TaskItemCard(
    task: Task,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit,
    translate: (String, String) -> String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { onClick() }
            .testTag("task_item_card_${task.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (task.isCompleted) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (task.isCompleted) 0.dp else 2.dp
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (task.isCompleted) {
                Color.Transparent
            } else {
                MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Elegant completion Checkbox with anim
            IconButton(
                onClick = onToggle,
                modifier = Modifier.testTag("task_checkbox_${task.id}")
            ) {
                Icon(
                    imageVector = if (task.isCompleted) Icons.Default.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                    contentDescription = "Toggle completion",
                    tint = if (task.isCompleted) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    },
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Task content Details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = task.title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = if (task.isCompleted) {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Metadata Badges (Category and Priority)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Category icon + text badge
                    val catIcon = when (task.category) {
                        "Personal" -> Icons.Outlined.Person
                        "Work" -> Icons.Outlined.Work
                        "Shopping" -> Icons.Outlined.ShoppingCart
                        "Health" -> Icons.Outlined.Favorite
                        else -> Icons.Outlined.Label
                    }
                    val catText = when (task.category) {
                        "Personal" -> translate("ব্যক্তিগত", "Personal")
                        "Work" -> translate("কাজ", "Work")
                        "Shopping" -> translate("কেনাকাটা", "Shopping")
                        "Health" -> translate("স্বাস্থ্য", "Health")
                        else -> task.category
                    }

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = catIcon,
                            contentDescription = null,
                            modifier = Modifier.size(10.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = catText,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Priority Badge
                    val prioText = when (task.priority) {
                        "High" -> translate("উচ্চ", "High")
                        "Medium" -> translate("মাঝারি", "Medium")
                        "Low" -> translate("নিম্ন", "Low")
                        else -> task.priority
                    }
                    val prioColor = when (task.priority) {
                        "High" -> Color(0xFFEF5350)
                        "Medium" -> Color(0xFFFFB74D)
                        else -> Color(0xFF4DB6AC)
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(prioColor.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = prioText,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = prioColor
                        )
                    }
                }
            }

            // Delete action button
            IconButton(
                onClick = onDelete,
                modifier = Modifier.testTag("delete_button_${task.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Delete task",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// Stunning Individual Note Grid Item Card
@Composable
fun NoteItemCard(
    note: Note,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val parsedColor = remember(note.colorHex) {
        try {
            Color(android.graphics.Color.parseColor(note.colorHex))
        } catch (e: Exception) {
            Color(0xFFF5F5F5)
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = parsedColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(width = 1.dp, color = Color.Black.copy(alpha = 0.05f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = note.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.DarkGray,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete note",
                        tint = Color.DarkGray.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = note.content,
                fontSize = 12.sp,
                color = Color.DarkGray.copy(alpha = 0.8f),
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            val noteDateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(note.createdAt))
            Text(
                text = noteDateStr,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = Color.DarkGray.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}
