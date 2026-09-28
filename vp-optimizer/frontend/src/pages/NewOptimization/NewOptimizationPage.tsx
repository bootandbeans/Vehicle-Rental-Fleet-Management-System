import { useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import Alert from '@mui/material/Alert';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import Stack from '@mui/material/Stack';
import Step from '@mui/material/Step';
import StepLabel from '@mui/material/StepLabel';
import Stepper from '@mui/material/Stepper';
import Typography from '@mui/material/Typography';
import { PageHeader } from '../../components/common/PageHeader';
import { ErrorState, LoadingState } from '../../components/common/StateBlocks';
import { OptimizationForm } from '../../components/optimization/OptimizationForm';
import type { OptimizationFormValues } from '../../components/optimization/OptimizationForm';
import { ProductSelector } from '../../components/optimization/ProductSelector';
import type { SelectionMap } from '../../components/optimization/ProductSelector';
import { useApp } from '../../context/AppContext';
import { useAsync } from '../../hooks/useAsync';
import { messageOf } from '../../services/errors';
import type { OptimizationRunResponse } from '../../types/optimization';

/**
 * Two step flow: pick the eligible products, then configure and run the optimization.
 *
 * The product selection is a first class citizen - it is what the engine receives, and nothing else.
 */
export function NewOptimizationPage() {
  const { api, settings } = useApp();
  const navigate = useNavigate();
  const [activeStep, setActiveStep] = useState(0);
  const [selection, setSelection] = useState<SelectionMap>({});
  const [submitting, setSubmitting] = useState(false);
  const [runError, setRunError] = useState<string | null>(null);

  const productsState = useAsync(() => api.products.selectable(), [api]);
  const categoriesState = useAsync(() => api.categories.list(), [api]);

  const selectedProducts = useMemo(
    () => (productsState.data ?? []).filter((product) => selection[product.id] !== undefined),
    [productsState.data, selection],
  );
  const requiredCount = selectedProducts.filter((product) => selection[product.id] === 'REQUIRED').length;

  const handleRun = async (values: OptimizationFormValues) => {
    setSubmitting(true);
    setRunError(null);
    try {
      const response: OptimizationRunResponse = await api.optimization.run({
        productIds: selectedProducts.map((product) => product.id),
        requiredProductIds: selectedProducts
          .filter((product) => selection[product.id] === 'REQUIRED')
          .map((product) => product.id),
        discountPercent: Number(values.discountPercent),
        gstPercent: Number(values.gstPercent),
        targetVp: Number(values.targetVp),
        toleranceType: values.toleranceType,
        toleranceValue: Number(values.toleranceValue),
        resultLimit: Number(values.resultLimit) || settings.resultLimit,
        name: values.name?.trim() ? values.name.trim() : null,
      });
      navigate(`/results/${response.sessionId}`);
    } catch (error) {
      setRunError(messageOf(error));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <>
      <PageHeader
        title="New optimization"
        subtitle="Select the products that may be used, set your target VP and let the engine find the best combinations."
      />

      <Stepper activeStep={activeStep} sx={{ mb: 2 }}>
        <Step>
          <StepLabel>Select products</StepLabel>
        </Step>
        <Step>
          <StepLabel>Target and pricing</StepLabel>
        </Step>
        <Step>
          <StepLabel>Results</StepLabel>
        </Step>
      </Stepper>

      {productsState.loading && <LoadingState label="Loading product catalogue…" />}
      {productsState.error && <ErrorState message={productsState.error} onRetry={productsState.reload} />}

      {!productsState.loading && !productsState.error && activeStep === 0 && (
        <Card>
          <CardContent>
            <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
              Only the products you tick here are sent to the optimizer. Mark a product as{' '}
              <strong>required</strong> when it must appear in every combination.
            </Typography>
            <ProductSelector
              products={productsState.data ?? []}
              selection={selection}
              onChange={setSelection}
              categories={categoriesState.data ?? []}
            />
            <Stack direction="row" spacing={1} sx={{ mt: 3 }}>
              <Button variant="contained" disabled={selectedProducts.length === 0} onClick={() => setActiveStep(1)}>
                Continue ({selectedProducts.length} selected)
              </Button>
              <Box sx={{ flexGrow: 1 }} />
            </Stack>
          </CardContent>
        </Card>
      )}

      {activeStep === 1 && (
        <Card>
          <CardContent>
            <Alert severity="info" sx={{ mb: 2 }}>
              {selectedProducts.length} product{selectedProducts.length === 1 ? '' : 's'} selected
              {requiredCount > 0 ? `, ${requiredCount} required` : ''} — you can go back to change the selection.
            </Alert>
            <OptimizationForm
              defaultValues={{
                discountPercent: settings.discountPercent,
                gstPercent: settings.gstPercent,
                targetVp: 500,
                toleranceType: settings.toleranceType,
                toleranceValue: settings.toleranceValue,
                resultLimit: settings.resultLimit,
                name: '',
              }}
              selectedCount={selectedProducts.length}
              requiredCount={requiredCount}
              submitting={submitting}
              error={runError}
              onSubmit={handleRun}
            />
            <Stack direction="row" spacing={1} sx={{ mt: 2 }}>
              <Button color="inherit" onClick={() => setActiveStep(0)} disabled={submitting}>
                Back to selection
              </Button>
            </Stack>
          </CardContent>
        </Card>
      )}
    </>
  );
}
