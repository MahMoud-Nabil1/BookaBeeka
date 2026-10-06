import { useState } from 'react';
import { Trash2, Pencil, Plus } from 'lucide-react';
import PageLayout from '../../../../components/layout/PageLayout';
import { Button } from '../../../../components/ui/button';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '../../../../components/ui/table';
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from '../../../../components/ui/popover';
import { Badge } from '../../../../components/ui/badge';
import {
  useAmenities,
  useDeleteAmenity,
} from '../../../catalog/hooks/useAmenities';
import { CreateAmenityModal } from '../components/CreateAmenityModal';
import { EditAmenityModal } from '../components/EditAmenityModal';
import type { AmenityResponse } from '../../../../types/inventory';

export function AdminAmenitiesPage() {
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
  const [editingAmenity, setEditingAmenity] = useState<AmenityResponse | null>(
    null
  );
  const [deleteConfirmId, setDeleteConfirmId] = useState<string | null>(null);

  const { data: amenities = [], isLoading } = useAmenities();
  const deleteAmenity = useDeleteAmenity();

  const handleDelete = (id: string) => {
    deleteAmenity.mutate(id, {
      onSuccess: () => setDeleteConfirmId(null),
    });
  };

  return (
    <PageLayout
      title="Amenities"
      description="Manage hotel amenities and facilities"
    >
      <div className="space-y-6">
        <div className="flex justify-between items-center">
          <div>
            <p className="text-sm text-muted-foreground">
              Create and manage amenities that can be assigned to rooms
            </p>
          </div>
          <Button onClick={() => setIsCreateModalOpen(true)}>
            <Plus className="mr-2 h-4 w-4" />
            Add Amenity
          </Button>
        </div>

        {isLoading ? (
          <div className="flex items-center justify-center py-12">
            <div className="text-muted-foreground">Loading amenities...</div>
          </div>
        ) : amenities.length === 0 ? (
          <div className="flex flex-col items-center justify-center py-12 border border-dashed rounded-lg">
            <p className="text-muted-foreground mb-4">No amenities found</p>
            <Button onClick={() => setIsCreateModalOpen(true)} variant="outline">
              <Plus className="mr-2 h-4 w-4" />
              Create Your First Amenity
            </Button>
          </div>
        ) : (
          <div className="border rounded-lg">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Name</TableHead>
                  <TableHead>Icon</TableHead>
                  <TableHead>Description</TableHead>
                  <TableHead className="text-right">Actions</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {amenities.map((amenity) => (
                  <TableRow key={amenity.id}>
                    <TableCell className="font-medium">
                      {amenity.name}
                    </TableCell>
                    <TableCell>
                      {amenity.icon ? (
                        <Badge variant="secondary">{amenity.icon}</Badge>
                      ) : (
                        <span className="text-muted-foreground text-sm">—</span>
                      )}
                    </TableCell>
                    <TableCell>
                      <div className="max-w-md truncate">
                        {amenity.description || (
                          <span className="text-muted-foreground text-sm">
                            No description
                          </span>
                        )}
                      </div>
                    </TableCell>
                    <TableCell className="text-right">
                      <div className="flex justify-end gap-2">
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => setEditingAmenity(amenity)}
                        >
                          <Pencil className="h-4 w-4" />
                        </Button>
                        <Popover
                          open={deleteConfirmId === amenity.id}
                          onOpenChange={(open) =>
                            setDeleteConfirmId(open ? amenity.id : null)
                          }
                        >
                          <PopoverTrigger asChild>
                            <Button variant="ghost" size="sm">
                              <Trash2 className="h-4 w-4 text-destructive" />
                            </Button>
                          </PopoverTrigger>
                          <PopoverContent className="w-80">
                            <div className="space-y-4">
                              <div>
                                <h4 className="font-semibold">Delete Amenity</h4>
                                <p className="text-sm text-muted-foreground mt-1">
                                  Are you sure you want to delete "{amenity.name}
                                  "? This will unlink it from all rooms.
                                </p>
                              </div>
                              <div className="flex justify-end gap-2">
                                <Button
                                  variant="outline"
                                  size="sm"
                                  onClick={() => setDeleteConfirmId(null)}
                                >
                                  Cancel
                                </Button>
                                <Button
                                  variant="destructive"
                                  size="sm"
                                  onClick={() => handleDelete(amenity.id)}
                                  disabled={deleteAmenity.isPending}
                                >
                                  {deleteAmenity.isPending
                                    ? 'Deleting...'
                                    : 'Delete'}
                                </Button>
                              </div>
                            </div>
                          </PopoverContent>
                        </Popover>
                      </div>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </div>
        )}
      </div>

      <CreateAmenityModal
        open={isCreateModalOpen}
        onOpenChange={setIsCreateModalOpen}
      />

      {editingAmenity && (
        <EditAmenityModal
          amenity={editingAmenity}
          open={!!editingAmenity}
          onOpenChange={(open) => !open && setEditingAmenity(null)}
        />
      )}
    </PageLayout>
  );
}
