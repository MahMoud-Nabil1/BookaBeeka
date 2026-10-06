import { useRef, useState } from 'react';
import { Star, Trash2, Upload, ChevronUp, ChevronDown, Loader2 } from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from '../../../../components/ui/card';
import { Button } from '../../../../components/ui/button';
import { Badge } from '../../../../components/ui/badge';
import { Input } from '../../../../components/ui/input';
import {
  useRoomPhotos,
  useUploadPhoto,
  useDeletePhoto,
  useSetPrimaryPhoto,
  useReorderPhotos,
} from '../../../catalog/hooks/useMedia';
import type { MediaPhotoResponse } from '../../../../types/media';

interface RoomPhotoManagerProps {
  resourceId: string;
}

export function RoomPhotoManager({ resourceId }: RoomPhotoManagerProps) {
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [uploadingFile, setUploadingFile] = useState<string | null>(null);

  const { data: photos = [], isLoading } = useRoomPhotos(resourceId);
  const uploadPhoto = useUploadPhoto(resourceId);
  const deletePhoto = useDeletePhoto(resourceId);
  const setPrimary = useSetPrimaryPhoto(resourceId);
  const reorderPhotos = useReorderPhotos(resourceId);

  // Sort photos by sortOrder
  const sortedPhotos = [...photos].sort((a, b) => a.sortOrder - b.sortOrder);

  const handleFileSelect = async (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (!file) return;

    // Validate file type
    if (!file.type.startsWith('image/')) {
      alert('Please select an image file');
      return;
    }

    // Validate file size (max 10MB)
    if (file.size > 10 * 1024 * 1024) {
      alert('File size must be less than 10MB');
      return;
    }

    setUploadingFile(file.name);
    
    try {
      await uploadPhoto.mutateAsync({
        file,
        isPrimary: photos.length === 0, // First photo is primary by default
      });
    } finally {
      setUploadingFile(null);
      // Reset the input so the same file can be selected again
      if (fileInputRef.current) {
        fileInputRef.current.value = '';
      }
    }
  };

  const handleDelete = async (photoId: string) => {
    if (!confirm('Are you sure you want to delete this photo?')) return;
    await deletePhoto.mutateAsync(photoId);
  };

  const handleSetPrimary = async (photoId: string) => {
    await setPrimary.mutateAsync(photoId);
  };

  const handleMoveUp = (index: number) => {
    if (index === 0) return;

    const newOrder = [...sortedPhotos];
    [newOrder[index - 1], newOrder[index]] = [newOrder[index], newOrder[index - 1]];

    reorderPhotos.mutate({
      photoIds: newOrder.map(p => p.id),
    });
  };

  const handleMoveDown = (index: number) => {
    if (index === sortedPhotos.length - 1) return;

    const newOrder = [...sortedPhotos];
    [newOrder[index], newOrder[index + 1]] = [newOrder[index + 1], newOrder[index]];

    reorderPhotos.mutate({
      photoIds: newOrder.map(p => p.id),
    });
  };

  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center justify-between">
          <span>Room Photos</span>
          <Button
            size="sm"
            onClick={() => fileInputRef.current?.click()}
            disabled={uploadPhoto.isPending}
          >
            {uploadPhoto.isPending ? (
              <>
                <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                Uploading...
              </>
            ) : (
              <>
                <Upload className="mr-2 h-4 w-4" />
                Upload Photo
              </>
            )}
          </Button>
        </CardTitle>
      </CardHeader>

      <CardContent>
        <Input
          ref={fileInputRef}
          type="file"
          accept="image/*"
          onChange={handleFileSelect}
          className="hidden"
        />

        {isLoading ? (
          <div className="flex justify-center py-8">
            <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
          </div>
        ) : sortedPhotos.length === 0 ? (
          <div className="text-center py-8 text-muted-foreground">
            <p>No photos yet</p>
            <p className="text-sm mt-1">Upload your first photo to get started</p>
          </div>
        ) : (
          <div className="space-y-3">
            {uploadingFile && (
              <div className="p-4 border rounded-lg bg-muted/50">
                <div className="flex items-center gap-2">
                  <Loader2 className="h-4 w-4 animate-spin" />
                  <span className="text-sm">Uploading {uploadingFile}...</span>
                </div>
              </div>
            )}

            {sortedPhotos.map((photo, index) => (
              <PhotoItem
                key={photo.id}
                photo={photo}
                index={index}
                totalPhotos={sortedPhotos.length}
                onDelete={() => handleDelete(photo.id)}
                onSetPrimary={() => handleSetPrimary(photo.id)}
                onMoveUp={() => handleMoveUp(index)}
                onMoveDown={() => handleMoveDown(index)}
                isDeleting={deletePhoto.isPending}
                isSettingPrimary={setPrimary.isPending}
              />
            ))}
          </div>
        )}
      </CardContent>
    </Card>
  );
}

interface PhotoItemProps {
  photo: MediaPhotoResponse;
  index: number;
  totalPhotos: number;
  onDelete: () => void;
  onSetPrimary: () => void;
  onMoveUp: () => void;
  onMoveDown: () => void;
  isDeleting: boolean;
  isSettingPrimary: boolean;
}

function PhotoItem({
  photo,
  index,
  totalPhotos,
  onDelete,
  onSetPrimary,
  onMoveUp,
  onMoveDown,
  isDeleting,
  isSettingPrimary,
}: PhotoItemProps) {
  return (
    <div className="flex items-center gap-3 p-3 border rounded-lg bg-card hover:bg-muted/50 transition-colors">
      {/* Thumbnail */}
      <div className="relative flex-shrink-0">
        <img
          src={photo.url}
          alt={`Room photo ${index + 1}`}
          className="w-20 h-20 object-cover rounded"
        />
        {photo.isPrimary && (
          <Badge className="absolute -top-2 -right-2 bg-yellow-500 hover:bg-yellow-600">
            Primary
          </Badge>
        )}
      </div>

      {/* Info */}
      <div className="flex-1 min-w-0">
        <p className="text-sm font-medium truncate">Photo {index + 1}</p>
        <p className="text-xs text-muted-foreground truncate">{photo.publicId}</p>
      </div>

      {/* Actions */}
      <div className="flex items-center gap-1">
        {/* Reorder buttons */}
        <div className="flex flex-col gap-0.5">
          <Button
            size="sm"
            variant="ghost"
            onClick={onMoveUp}
            disabled={index === 0}
            className="h-5 w-5 p-0"
          >
            <ChevronUp className="h-3 w-3" />
          </Button>
          <Button
            size="sm"
            variant="ghost"
            onClick={onMoveDown}
            disabled={index === totalPhotos - 1}
            className="h-5 w-5 p-0"
          >
            <ChevronDown className="h-3 w-3" />
          </Button>
        </div>

        {/* Set Primary button */}
        <Button
          size="sm"
          variant="ghost"
          onClick={onSetPrimary}
          disabled={photo.isPrimary || isSettingPrimary}
          title="Set as primary photo"
        >
          <Star
            className={`h-4 w-4 ${photo.isPrimary ? 'fill-yellow-500 text-yellow-500' : ''}`}
          />
        </Button>

        {/* Delete button */}
        <Button
          size="sm"
          variant="ghost"
          onClick={onDelete}
          disabled={isDeleting}
          className="text-destructive hover:text-destructive"
          title="Delete photo"
        >
          <Trash2 className="h-4 w-4" />
        </Button>
      </div>
    </div>
  );
}
