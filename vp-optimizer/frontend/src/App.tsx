import { Navigate, Route, Routes } from 'react-router-dom';
import { AppLayout } from './components/layout/AppLayout';
import { DashboardPage } from './pages/Dashboard/DashboardPage';
import { ProductsPage } from './pages/Products/ProductsPage';
import { CategoriesPage } from './pages/Categories/CategoriesPage';
import { NewOptimizationPage } from './pages/NewOptimization/NewOptimizationPage';
import { OptimizationResultsPage } from './pages/OptimizationResults/OptimizationResultsPage';
import { OptimizationHistoryPage } from './pages/OptimizationHistory/OptimizationHistoryPage';
import { SettingsPage } from './pages/Settings/SettingsPage';

export default function App() {
  return (
    <AppLayout>
      <Routes>
        <Route path="/" element={<DashboardPage />} />
        <Route path="/products" element={<ProductsPage />} />
        <Route path="/categories" element={<CategoriesPage />} />
        <Route path="/optimize" element={<NewOptimizationPage />} />
        <Route path="/results/:sessionId" element={<OptimizationResultsPage />} />
        <Route path="/history" element={<OptimizationHistoryPage />} />
        <Route path="/settings" element={<SettingsPage />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </AppLayout>
  );
}
