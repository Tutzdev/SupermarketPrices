import * as Dialog from "@radix-ui/react-dialog";
import { NativeButton } from "@/components/ui/native-button";
import { InlineError } from "@/components/ui/feedback";

interface ConfirmDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  title: string;
  description: string;
  confirmLabel: string;
  loading?: boolean;
  error?: string;
  onConfirm: () => void;
}

export function ConfirmDialog({ open, onOpenChange, title, description, confirmLabel, loading, error, onConfirm }: ConfirmDialogProps) {
  return (
    <Dialog.Root open={open} onOpenChange={onOpenChange}>
      <Dialog.Portal>
        <Dialog.Overlay className="fixed inset-0 z-50 bg-black/45" />
        <Dialog.Content className="fixed left-1/2 top-1/2 z-50 w-[min(92vw,28rem)] -translate-x-1/2 -translate-y-1/2 rounded-xl border border-border bg-white p-6 shadow-2xl focus:outline-none">
          <Dialog.Title className="text-lg font-bold">{title}</Dialog.Title>
          <Dialog.Description className="mt-2 text-sm leading-6 text-muted">{description}</Dialog.Description>
          <InlineError>{error}</InlineError>
          <div className="mt-6 flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
            <Dialog.Close asChild><NativeButton variant="secondary">Cancelar</NativeButton></Dialog.Close>
            <NativeButton variant="danger" loading={loading} onClick={onConfirm}>{confirmLabel}</NativeButton>
          </div>
        </Dialog.Content>
      </Dialog.Portal>
    </Dialog.Root>
  );
}
