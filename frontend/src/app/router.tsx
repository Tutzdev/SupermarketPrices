import { Navigate, createBrowserRouter } from "react-router-dom";
import { AppShell } from "@/components/layout/app-shell";
import { LoadingState } from "@/components/ui/feedback";
import { EmailConfirmationPage, AuthPage, PasswordResetConfirmPage, PasswordResetRequestPage } from "@/features/auth/auth-page";
import { useAuth } from "@/features/auth/auth-context";
import { AdminPage } from "@/routes/app/admin-page";
import { AlertsPage } from "@/routes/app/alerts-page";
import { ComparisonPage } from "@/routes/app/comparison-page";
import { ContributionsPage } from "@/routes/app/contributions-page";
import { NotificationsPage } from "@/routes/app/notifications-page";
import { OverviewPage } from "@/routes/app/overview-page";
import { ProductDetailPage } from "@/routes/app/product-detail-page";
import { ProductsPage } from "@/routes/app/products-page";
import { ProfilePage } from "@/routes/app/profile-page";
import { ShoppingListDetailPage } from "@/routes/app/shopping-list-detail-page";
import { ShoppingListsPage } from "@/routes/app/shopping-lists-page";
import { StoresPage } from "@/routes/app/stores-page";
import { AccessDeniedPage, NotFoundPage } from "@/routes/error-pages";
import { LandingPage } from "@/routes/landing-page";
import { SubscriptionPage } from "@/routes/subscription-page";

function ProtectedLayout() {
  const { user, loading } = useAuth();
  if (loading) return <main className="grid min-h-screen place-items-center p-4"><LoadingState label="Carregando sua conta…" /></main>;
  if (!user) return <Navigate to="/entrar" replace />;
  return <AppShell />;
}

function AdminGuard() {
  const { user } = useAuth();
  return user?.role === "ADMIN" ? <AdminPage /> : <Navigate to="/acesso-negado" replace />;
}

export const router = createBrowserRouter([
  { path: "/", element: <LandingPage /> },
  { path: "/entrar", element: <AuthPage /> },
  { path: "/recuperar-senha", element: <PasswordResetRequestPage /> },
  { path: "/redefinir-senha", element: <PasswordResetConfirmPage /> },
  { path: "/confirmar-email", element: <EmailConfirmationPage /> },
  { path: "/assinar", element: <SubscriptionPage /> },
  { path: "/acesso-negado", element: <AccessDeniedPage /> },
  {
    path: "/app",
    element: <ProtectedLayout />,
    children: [
      { index: true, element: <OverviewPage /> },
      { path: "comparar", element: <ComparisonPage /> },
      { path: "produtos", element: <ProductsPage /> },
      { path: "produtos/:id", element: <ProductDetailPage /> },
      { path: "supermercados", element: <StoresPage /> },
      { path: "listas", element: <ShoppingListsPage /> },
      { path: "listas/:id", element: <ShoppingListDetailPage /> },
      { path: "alertas", element: <AlertsPage /> },
      { path: "contribuir", element: <ContributionsPage /> },
      { path: "notificacoes", element: <NotificationsPage /> },
      { path: "perfil", element: <ProfilePage /> },
      { path: "admin", element: <AdminGuard /> },
    ],
  },
  { path: "*", element: <NotFoundPage /> },
]);
