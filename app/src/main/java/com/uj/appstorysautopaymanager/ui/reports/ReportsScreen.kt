package com.uj.appstorysautopaymanager.ui.reports

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uj.appstorysautopaymanager.ui.components.AppHeader
import com.uj.appstorysautopaymanager.ui.components.PremiumNormalCard
import com.uj.appstorysautopaymanager.ui.dashboard.DashboardViewModel
import com.uj.appstorysautopaymanager.ui.theme.*

@Composable
fun ReportsScreen(
    dashboardViewModel: DashboardViewModel,
    currencySymbol: String = "₹"
) {
    val stats by dashboardViewModel.dashboardStats.collectAsState()
    val allTxns by dashboardViewModel.allTransactions.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        contentPadding = PaddingValues(top = 24.dp, bottom = 100.dp)
    ) {
        item {
            AppHeader(
                title = "Analytics",
                subtitle = "Visual insights into your transactions & bills"
            )
        }

        // Section: Category Spending (Donut Chart)
        item {
            PremiumNormalCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Category Distribution",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )
                Spacer(modifier = Modifier.height(16.dp))

                if (stats.categorySummary.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No transaction data available", color = TextGray)
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Donut Canvas
                        val chartData = stats.categorySummary
                        val total = chartData.values.sum()
                        val colors = listOf(PrimaryIndigo, SecondaryPurple, AccentTeal, AccentCoral, AccentEmerald, Color(0xFFF59E0B))

                        Canvas(
                            modifier = Modifier
                                .size(140.dp)
                                .weight(1.2f)
                        ) {
                            var startAngle = -90f
                            chartData.toList().forEachIndexed { index, (_, value) ->
                                val sweepAngle = (value.toFloat() / total.toFloat()) * 360f
                                val color = colors[index % colors.size]
                                drawArc(
                                    color = color,
                                    startAngle = startAngle,
                                    sweepAngle = sweepAngle,
                                    useCenter = false,
                                    style = Stroke(width = 24.dp.toPx(), cap = StrokeCap.Round)
                                )
                                startAngle += sweepAngle
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // Legend
                        Column(
                            modifier = Modifier.weight(1.8f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            chartData.toList().take(4).forEachIndexed { index, (category, amt) ->
                                val color = colors[index % colors.size]
                                val percentage = (amt / total) * 100
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                    )
                                    Text(
                                        text = "$category: %.0f%%".format(percentage),
                                        color = TextWhite,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section: Monthly Expenditure Trend (Bar Chart)
        item {
            PremiumNormalCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Spending Trend (Past 4 Months)",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )
                Spacer(modifier = Modifier.height(24.dp))

                // Dummy / Mock Monthly sums (since dynamic past months scan depends on inbox range)
                val monthlyData = listOf(
                    "Apr" to 12500f,
                    "May" to 18400f,
                    "Jun" to 9200f,
                    "Jul" to stats.monthlySpending.toFloat()
                )
                val maxVal = monthlyData.maxOf { it.second }.coerceAtLeast(1000f)
                val barColors = listOf(SecondaryPurple, PrimaryIndigo)
                val lineBorderColor = BorderColor

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .padding(horizontal = 8.dp)
                ) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height
                    val barWidth = 40.dp.toPx()
                    val spacing = (canvasWidth - (barWidth * monthlyData.size)) / (monthlyData.size + 1)

                    monthlyData.forEachIndexed { index, (month, valAmt) ->
                        val barHeight = (valAmt / maxVal) * (canvasHeight - 40.dp.toPx())
                        val x = spacing + index * (barWidth + spacing)
                        val y = canvasHeight - 20.dp.toPx() - barHeight

                        // Draw Bar
                        drawRect(
                            brush = Brush.verticalGradient(barColors),
                            topLeft = Offset(x, y),
                            size = Size(barWidth, barHeight)
                        )

                        // Draw Month Label below the bar
                        // Since native text rendering in canvas requires Paint/NativeCanvas,
                        // we can approximate it or just leave coordinate grids.
                        // Let's do simple line markers as baseline
                        drawLine(
                            color = lineBorderColor,
                            start = Offset(0f, canvasHeight - 20.dp.toPx()),
                            end = Offset(canvasWidth, canvasHeight - 20.dp.toPx()),
                            strokeWidth = 2f
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Row of month labels matching the canvas spacing
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    monthlyData.forEach { (month, amt) ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = month, color = TextWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(text = "$currencySymbol%.0f".format(amt), color = TextGray, fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }
}
