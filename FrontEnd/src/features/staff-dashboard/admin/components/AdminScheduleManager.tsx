import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Plus, Trash2, Loader2, Clock } from 'lucide-react';
import { toast } from 'sonner';
import { availabilityApi } from '../../../catalog/api/availabilityApi';
import { useAppSelector } from '../../../../redux/hooks';
import { selectTenantId } from '../../../../redux/selectors/authSelectors';
import { useResources } from '../../../catalog/hooks/useInventory';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import {
  Dialog, DialogContent, DialogFooter,
  DialogHeader, DialogTitle, DialogTrigger,
} from '@/components/ui/dialog';
import { Badge } from '@/components/ui/badge';

const DAY_NAMES: Record<number, string> = {
  1: 'Monday', 2: 'Tuesday', 3: 'Wednesday', 4: 'Thursday',
  5: 'Friday', 6: 'Saturday', 7: 'Sunday',
};

export default function AdminScheduleManager() {
  const tenantId = useAppSelector(selectTenantId);
  const queryClient = useQueryClient();

  const { data: resources = [] } = useResources(tenantId ?? undefined);
  const [selectedResourceId, setSelectedResourceId] = useState<string>('');

  // Schedule rules for selected room
  const { data: rules = [], isLoading: rulesLoading } = useQuery({
    queryKey: ['schedule-rules', selectedResourceId],
    queryFn: () => availabilityApi.getScheduleRules(selectedResourceId),
    enabled: !!selectedResourceId,
  });

  // New rule form state
  const [newDay, setNewDay] = useState('1');
  const [newStart, setNewStart] = useState('09:00');
  const [newEnd, setNewEnd] = useState('17:00');
  const [addOpen, setAddOpen] = useState(false);

  const addMutation = useMutation({
    mutationFn: () =>
      availabilityApi.createScheduleRule(tenantId!, selectedResourceId, {
        dayOfWeek: newDay,
        startTime: newStart,
        endTime: newEnd,
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['schedule-rules', selectedResourceId] });
      toast.success('Schedule rule added.');
      setAddOpen(false);
    },
    onError: () => toast.error('Failed to add schedule rule.'),
  });

  const deleteMutation = useMutation({
    mutationFn: (ruleId: string) => availabilityApi.deleteScheduleRule(ruleId, tenantId!),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['schedule-rules', selectedResourceId] });
      toast.success('Rule removed.');
    },
    onError: () => toast.error('Failed to remove rule.'),
  });

  return (
    <Card className="border-border">
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <Clock className="h-5 w-5 text-muted-foreground" />
          Schedule Rules
        </CardTitle>
        <CardDescription>
          Define weekly working hours for each room.
        </CardDescription>
      </CardHeader>
      <CardContent className="space-y-4">
        {/* Room selector */}
        <div className="space-y-1.5">
          <Label>Room</Label>
          <Select value={selectedResourceId} onValueChange={setSelectedResourceId}>
            <SelectTrigger className="w-full max-w-sm">
              <SelectValue placeholder="Select a room…" />
            </SelectTrigger>
            <SelectContent>
              {resources.map((r) => (
                <SelectItem key={r.id} value={r.id}>{r.name}</SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>

        {selectedResourceId && (
          <>
            {/* Rules list */}
            {rulesLoading ? (
              <div className="flex items-center gap-2 text-muted-foreground py-4">
                <Loader2 className="h-4 w-4 animate-spin" />
                <span className="text-sm">Loading rules…</span>
              </div>
            ) : rules.length === 0 ? (
              <p className="text-sm text-muted-foreground py-4">
                No schedule rules defined. Add one below.
              </p>
            ) : (
              <div className="space-y-2">
                {rules.map((rule: { id: string; dayOfWeek: number; startTime: string; endTime: string }) => (
                  <div
                    key={rule.id}
                    className="flex items-center justify-between gap-3 p-3 rounded-lg border border-border bg-muted/20"
                  >
                    <div className="flex items-center gap-3">
                      <Badge variant="secondary" className="w-24 justify-center">
                        {DAY_NAMES[rule.dayOfWeek] ?? rule.dayOfWeek}
                      </Badge>
                      <span className="text-sm text-foreground font-medium">
                        {rule.startTime} – {rule.endTime}
                      </span>
                    </div>
                    <Button
                      variant="ghost"
                      size="icon"
                      className="h-8 w-8 text-destructive hover:bg-destructive/10"
                      onClick={() => deleteMutation.mutate(rule.id)}
                      disabled={deleteMutation.isPending}
                    >
                      <Trash2 className="h-4 w-4" />
                    </Button>
                  </div>
                ))}
              </div>
            )}

            {/* Add rule dialog */}
            <Dialog open={addOpen} onOpenChange={setAddOpen}>
              <DialogTrigger asChild>
                <Button variant="outline" size="sm" className="gap-2">
                  <Plus className="h-4 w-4" /> Add Rule
                </Button>
              </DialogTrigger>
              <DialogContent className="sm:max-w-sm">
                <DialogHeader>
                  <DialogTitle>Add Schedule Rule</DialogTitle>
                </DialogHeader>
                <div className="space-y-4 py-2">
                  <div className="space-y-1.5">
                    <Label>Day of Week</Label>
                    <Select value={newDay} onValueChange={setNewDay}>
                      <SelectTrigger>
                        <SelectValue />
                      </SelectTrigger>
                      <SelectContent>
                        {Object.entries(DAY_NAMES).map(([val, name]) => (
                          <SelectItem key={val} value={val}>{name}</SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  </div>
                  <div className="grid grid-cols-2 gap-3">
                    <div className="space-y-1.5">
                      <Label htmlFor="start-time">Start Time</Label>
                      <Input id="start-time" type="time" value={newStart} onChange={(e) => setNewStart(e.target.value)} />
                    </div>
                    <div className="space-y-1.5">
                      <Label htmlFor="end-time">End Time</Label>
                      <Input id="end-time" type="time" value={newEnd} onChange={(e) => setNewEnd(e.target.value)} />
                    </div>
                  </div>
                </div>
                <DialogFooter>
                  <Button variant="outline" onClick={() => setAddOpen(false)}>Cancel</Button>
                  <Button onClick={() => addMutation.mutate()} disabled={addMutation.isPending}>
                    {addMutation.isPending && <Loader2 className="h-4 w-4 animate-spin mr-2" />}
                    Add Rule
                  </Button>
                </DialogFooter>
              </DialogContent>
            </Dialog>
          </>
        )}
      </CardContent>
    </Card>
  );
}
