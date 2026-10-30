package com.example.myfinances

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myfinances.ui.theme.MyFinancesTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.time.LocalDate
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

class CategoryDetailsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val categoryName = intent.getStringExtra("CATEGORY_NAME") ?: return

        setContent {
            MyFinancesTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CategoryDetailsScreen(categoryName)
                }
            }
        }
    }
}

@Composable
fun CategoryDetailsScreen(categoryName: String) {
    var expensesList by remember { mutableStateOf<List<Expense>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    DisposableEffect(categoryName) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            isLoading = false
            return@DisposableEffect onDispose {}
        }

        val databaseRef =
            FirebaseDatabase.getInstance().getReference("Users").child(uid).child("Expenses")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val currentMonth = LocalDate.now().monthValue
                val currentYear = LocalDate.now().year

                val fetchedExpenses = mutableListOf<Expense>()
                for (child in snapshot.children) {
                    val catStr = child.child("category").getValue(String::class.java) ?: ""

                    if (catStr == categoryName) {
                        val id = child.key ?: ""
                        val name = child.child("name").getValue(String::class.java) ?: ""
                        val amount =
                            child.child("amount").getValue(Double::class.java)?.toFloat() ?: 0f
                        val dateStr =
                            child.child("date").getValue(String::class.java) ?: LocalDate.now()
                                .toString()

                        val date = LocalDate.parse(dateStr)

                        // FILTROWANIE: Bierzemy pod uwagę tylko wydatki z bieżącego miesiąca i roku
                        if (date.monthValue == currentMonth && date.year == currentYear) {
                            fetchedExpenses.add(
                                Expense(
                                    id,
                                    name,
                                    amount,
                                    date,
                                    ExpenseCategory.valueOf(catStr)
                                )
                            )
                        }
                    }
                }

                expensesList = fetchedExpenses.sortedByDescending { it.amount }
                isLoading = false
            }

            override fun onCancelled(error: DatabaseError) {
                isLoading = false
            }
        }
        databaseRef.addValueEventListener(listener)
        onDispose { databaseRef.removeEventListener(listener) }
    }

    // Dynamiczne generowanie kolorów (przelicza się ponownie tylko gdy zmieni się expensesList)
    val dynamicColors = remember(expensesList) {
        expensesList.indices.map { index ->
            // Używamy Złotego Kąta, nasycenia 0.7 i jasności 0.9 dla ładnych kolorów
            val hue = (index * 137.508f) % 360f
            Color.hsv(hue = hue, saturation = 0.7f, value = 0.9f)
        }
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        // Obliczamy sumę wcześniej, bo wyświetlamy ją na samej górze
        val totalSum = expensesList.sumOf { it.amount.toDouble() }.toFloat()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
            // Domyślnie elementy układają się do lewej krawędzi (jak w LinearLayout vertical bez gravity)
        ) {

            // 1. NAGŁÓWEK (odpowiednik tvCategoryTitle)
            Text(
                text = "Szczegóły: $categoryName",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
            )

            // 2. SUMA (odpowiednik tvTotalSum)
            Text(
                text = String.format("Razem: %.2f zł", totalSum),
                fontSize = 18.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )

            // 3. WYKRES (odpowiednik pieChartContainer) - centrujemy go horyzontalnie
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally) // wyśrodkowanie tylko wykresu
                    .padding(bottom = 16.dp) // Padding dodany PRZED size, aby działał jako margines zewnętrzny
                    .size(250.dp)            // Dzięki temu Canvas ma wewnątrz idealne 250x250 dp
            ) {
                var rotationAngle by remember { mutableFloatStateOf(0f) }

                val textPaint = remember {
                    android.graphics.Paint().apply {
                        color = android.graphics.Color.WHITE
                        textSize = 42f
                        textAlign = android.graphics.Paint.Align.CENTER
                        isFakeBoldText = true
                    }
                }

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            var lastAngle = 0f
                            detectDragGestures(
                                onDragStart = { offset ->
                                    val cx = size.width / 2f
                                    val cy = size.height / 2f
                                    lastAngle = Math.toDegrees(
                                        atan2(
                                            (offset.y - cy).toDouble(),
                                            (offset.x - cx).toDouble()
                                        )
                                    ).toFloat()
                                },
                                onDrag = { change, _ ->
                                    val cx = size.width / 2f
                                    val cy = size.height / 2f
                                    val currentAngle = Math.toDegrees(
                                        atan2(
                                            (change.position.y - cy).toDouble(),
                                            (change.position.x - cx).toDouble()
                                        )
                                    ).toFloat()

                                    rotationAngle += (currentAngle - lastAngle)
                                    lastAngle = currentAngle
                                }
                            )
                        }
                ) {
                    var startAngle = -90f + rotationAngle
                    val strokeWidthPx = 150f

                    val padding = strokeWidthPx / 2f
                    val arcSize = Size(size.width - strokeWidthPx, size.height - strokeWidthPx)
                    val radius = (size.width - strokeWidthPx) / 2f

                    expensesList.forEachIndexed { index, expense ->
                        val sweepAngle =
                            if (totalSum > 0) (expense.amount / totalSum) * 360f else 0f

                        drawArc(
                            color = dynamicColors[index],
                            startAngle = startAngle,
                            sweepAngle = sweepAngle,
                            useCenter = false,
                            style = Stroke(width = strokeWidthPx, cap = StrokeCap.Butt),
                            size = arcSize,
                            topLeft = Offset(padding, padding)
                        )

                        val percentage =
                            if (totalSum > 0) ((expense.amount / totalSum) * 100).toInt() else 0

                        if (percentage > 4) {
                            val middleAngle =
                                Math.toRadians((startAngle + sweepAngle / 2).toDouble())
                            val cx = size.width / 2f
                            val cy = size.height / 2f

                            val textX = cx + radius * cos(middleAngle).toFloat()
                            val textY =
                                cy + radius * sin(middleAngle).toFloat() - (textPaint.descent() + textPaint.ascent()) / 2

                            drawContext.canvas.nativeCanvas.drawText(
                                "$percentage%",
                                textX,
                                textY,
                                textPaint
                            )
                        }

                        startAngle += sweepAngle
                    }
                }
            } // Koniec Box z wykresem

            // 4. LISTA (odpowiednik ScrollView -> legendContainer)
            val context = LocalContext.current

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f) // zajmuje resztę dostępnego miejsca, co pozwala na przewijanie (jak w ScrollView)
            ) {
                items(expensesList.withIndex().toList()) { (index, expense) ->
                    val percentage =
                        if (totalSum > 0) ((expense.amount / totalSum) * 100).toInt() else 0
                    val itemColor = dynamicColors[index]

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val intent =
                                    Intent(context, ExpenseDetailActivity::class.java).apply {
                                        putExtra("EXTRA_ID", expense.id)
                                        putExtra("EXTRA_NAME", expense.name)
                                        putExtra("EXTRA_AMOUNT", expense.amount)
                                        putExtra("EXTRA_DATE", expense.date.toString())
                                        putExtra("EXTRA_CATEGORY", expense.category.name)
                                    }
                                context.startActivity(intent)
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .background(itemColor, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(text = expense.name, fontSize = 16.sp, color = Color.DarkGray)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = String.format("- %.2f zł", expense.amount),
                                color = itemColor
                            )
                            Text(text = "$percentage%", fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}