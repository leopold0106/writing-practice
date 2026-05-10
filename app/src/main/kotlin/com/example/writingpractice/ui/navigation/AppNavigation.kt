package com.example.writingpractice.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.writingpractice.ui.answerhistory.AnswerHistoryScreen
import com.example.writingpractice.ui.home.HomeScreen
import com.example.writingpractice.ui.notebook.NotebookScreen
import com.example.writingpractice.ui.notebookdetail.NotebookDetailScreen
import com.example.writingpractice.ui.practice.PracticeScreen
import com.example.writingpractice.ui.problemlist.ProblemListScreen
import com.example.writingpractice.ui.result.ResultScreen
import com.example.writingpractice.ui.settings.SettingsScreen
import com.example.writingpractice.ui.monthlytrend.MonthlyTrendScreen
import com.example.writingpractice.ui.weaknessanalysis.WeaknessAnalysisScreen

object Routes {
    const val HOME = "home"
    const val PROBLEM_LIST = "problem_list/{level}"
    const val PRACTICE = "practice?level={level}&problemId={problemId}"
    const val RESULT = "result/{answerId}"
    const val NOTEBOOK = "notebook"
    const val NOTEBOOK_DETAIL = "notebook_detail/{problemId}"
    const val SETTINGS = "settings"
    const val ANSWER_HISTORY = "answer_history/{problemId}"
    const val WEAKNESS_ANALYSIS = "weakness_analysis"
    const val MONTHLY_TREND = "monthly_trend"

    fun problemList(level: Int) = "problem_list/$level"
    fun practice(level: Int, problemId: Long? = null) =
        "practice?level=$level" + (problemId?.let { "&problemId=$it" } ?: "")
    fun result(answerId: Long) = "result/$answerId"
    fun notebookDetail(problemId: Long) = "notebook_detail/$problemId"
    fun answerHistory(problemId: Long) = "answer_history/$problemId"
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onLevelClick = { level -> navController.navigate(Routes.problemList(level)) },
                onNotebookClick = { navController.navigate(Routes.NOTEBOOK) },
                onSettingsClick = { navController.navigate(Routes.SETTINGS) },
                onWeaknessAnalysisClick = { navController.navigate(Routes.WEAKNESS_ANALYSIS) },
                onMonthlyTrendClick = { navController.navigate(Routes.MONTHLY_TREND) }
            )
        }

        composable(
            route = Routes.PROBLEM_LIST,
            arguments = listOf(navArgument("level") { type = NavType.IntType })
        ) { backStack ->
            val level = backStack.arguments?.getInt("level") ?: 1
            ProblemListScreen(
                level = level,
                onProblemClick = { problemId ->
                    navController.navigate(Routes.practice(level, problemId))
                },
                onAnswerClick = { problemId ->
                    navController.navigate(Routes.answerHistory(problemId))
                },
                onStartRandom = {
                    navController.navigate(Routes.practice(level))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.PRACTICE,
            arguments = listOf(
                navArgument("level") { type = NavType.IntType; defaultValue = 1 },
                navArgument("problemId") { type = NavType.LongType; defaultValue = -1L }
            )
        ) { backStack ->
            val level = backStack.arguments?.getInt("level") ?: 1
            val problemId = backStack.arguments?.getLong("problemId")?.takeIf { it != -1L }
            PracticeScreen(
                level = level,
                problemId = problemId,
                onResultReady = { answerId ->
                    navController.navigate(Routes.result(answerId)) {
                        popUpTo(Routes.HOME)
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.RESULT,
            arguments = listOf(navArgument("answerId") { type = NavType.LongType })
        ) { backStack ->
            val answerId = backStack.arguments?.getLong("answerId") ?: return@composable
            ResultScreen(
                answerId = answerId,
                onNextProblem = { level ->
                    navController.navigate(Routes.practice(level)) {
                        popUpTo(Routes.HOME)
                    }
                },
                onHome = { navController.popBackStack(Routes.HOME, inclusive = false) }
            )
        }

        composable(
            route = Routes.ANSWER_HISTORY,
            arguments = listOf(navArgument("problemId") { type = NavType.LongType })
        ) { backStack ->
            val problemId = backStack.arguments?.getLong("problemId") ?: return@composable
            AnswerHistoryScreen(
                onBack = { navController.popBackStack() },
                onAnswerClick = { answerId ->
                    navController.navigate(Routes.result(answerId))
                },
                onRePractice = { level ->
                    navController.navigate(Routes.practice(level, problemId)) {
                        popUpTo(Routes.HOME)
                    }
                }
            )
        }

        composable(Routes.NOTEBOOK) {
            NotebookScreen(
                onEntryClick = { problemId ->
                    navController.navigate(Routes.notebookDetail(problemId))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.NOTEBOOK_DETAIL,
            arguments = listOf(navArgument("problemId") { type = NavType.LongType })
        ) { backStack ->
            val problemId = backStack.arguments?.getLong("problemId") ?: return@composable
            NotebookDetailScreen(
                problemId = problemId,
                onRePractice = { level ->
                    navController.navigate(Routes.practice(level, problemId))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.WEAKNESS_ANALYSIS) {
            WeaknessAnalysisScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.MONTHLY_TREND) {
            MonthlyTrendScreen(onBack = { navController.popBackStack() })
        }
    }
}
